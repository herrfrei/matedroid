package com.matedroid.widget

import android.content.Context
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.matedroid.MainActivity
import com.matedroid.domain.model.CarImageResolver
import com.matedroid.ui.theme.CarColorPalette
import com.matedroid.ui.theme.CarColorPalettes
import com.matedroid.ui.util.GlowBitmapRenderer
import java.io.IOException
import kotlin.math.roundToInt

// Matches StatusSuccess / StatusError from Color.kt
private val ANDROID_STATUS_SUCCESS = android.graphics.Color.argb(230, 76, 175, 80)
private val ANDROID_STATUS_ERROR = android.graphics.Color.argb(255, 244, 67, 54)
private val ANDROID_STATUS_ERROR_DIM = android.graphics.Color.argb(178, 244, 67, 54)

/**
 * Home screen widget displaying real-time battery info for a configured car.
 *
 * All display data is persisted in Glance preferences so that [provideGlance]
 * can render real content without needing to inject [TeslamateRepository].
 * [updateWidget] writes every field from [CarWidgetDisplayData] into preferences
 * and then calls [update] to trigger a redraw.
 *
 * Uses [SizeMode.Exact] so the background bitmap is generated at the widget's
 * exact pixel dimensions, preventing any aspect-ratio distortion.
 */
class CarWidget : GlanceAppWidget() {

    companion object {
        // Glance preference keys — one per CarWidgetDisplayData field
        val CAR_ID_KEY = intPreferencesKey("car_id")
        val HAS_DATA_KEY = booleanPreferencesKey("has_data")
        val CAR_NAME_KEY = stringPreferencesKey("car_name")
        val EXTERIOR_COLOR_KEY = stringPreferencesKey("exterior_color")
        val MODEL_KEY = stringPreferencesKey("model")
        val TRIM_BADGING_KEY = stringPreferencesKey("trim_badging")
        val WHEEL_TYPE_KEY = stringPreferencesKey("wheel_type")
        val STATE_KEY = stringPreferencesKey("state")
        val IS_LOCKED_KEY = booleanPreferencesKey("is_locked")
        val SENTRY_MODE_KEY = booleanPreferencesKey("sentry_mode")
        val PLUGGED_IN_KEY = booleanPreferencesKey("plugged_in")
        val OUTSIDE_TEMP_KEY = floatPreferencesKey("outside_temp")   // Float.NaN if null
        val INSIDE_TEMP_KEY = floatPreferencesKey("inside_temp")     // Float.NaN if null
        val IS_CLIMATE_ON_KEY = booleanPreferencesKey("is_climate_on")
        val BATTERY_LEVEL_KEY = intPreferencesKey("battery_level")
        val RATED_RANGE_KEY = floatPreferencesKey("rated_range_km")  // -1 if null
        val CHARGE_LIMIT_KEY = intPreferencesKey("charge_limit_soc") // -1 if null
        val IS_CHARGING_KEY = booleanPreferencesKey("is_charging")
        val IS_DC_CHARGING_KEY = booleanPreferencesKey("is_dc_charging")
        val CHARGER_POWER_KEY = intPreferencesKey("charger_power")           // -1 if null
        val CHARGE_ENERGY_ADDED_KEY = floatPreferencesKey("charge_energy_added") // -1 if null
        val TIME_TO_FULL_KEY = floatPreferencesKey("time_to_full")           // -1 if null
        val CHARGER_VOLTAGE_KEY = intPreferencesKey("charger_voltage")       // -1 if null
        val CHARGER_CURRENT_KEY = intPreferencesKey("charger_current")       // -1 if null
        val AC_PHASES_KEY = intPreferencesKey("ac_phases")                   // -1 if null
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    // Exact mode: LocalSize.current returns the actual rendered widget dimensions so the
    // background bitmap can be generated without distortion.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val carId = prefs[CAR_ID_KEY]
        val hasData = prefs[HAS_DATA_KEY] ?: false

        provideContent {
            GlanceTheme {
                // Mirror the approach used by Glance's own Scaffold:
                // use system_app_widget_background_radius on API 31+, nothing on older devices
                // (pre-31 launchers don't clip widgets to rounded corners).
                val cornerMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
                } else {
                    GlanceModifier
                }
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .then(cornerMod)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    when {
                        carId == null -> {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(Color(0xFF1E2530)))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = LocalContext.current.getString(com.matedroid.R.string.widget_error_configure),
                                    style = TextStyle(color = ColorProvider(Color.White))
                                )
                            }
                        }

                        !hasData -> {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(Color(0xFF1E2530)))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = LocalContext.current.getString(com.matedroid.R.string.widget_loading),
                                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)))
                                )
                            }
                        }

                        else -> {
                            val ctx = LocalContext.current
                            val size = LocalSize.current
                            val density = ctx.resources.displayMetrics.density
                            val widthPx = (size.width.value * density).toInt().coerceAtLeast(100)
                            val heightPx = (size.height.value * density).toInt().coerceAtLeast(50)

                            // Bitmap generated at the exact widget pixel size — FillBounds is safe
                            val bgBitmap = buildBackgroundBitmap(ctx, prefs, widthPx, heightPx, density)
                            Image(
                                provider = ImageProvider(bgBitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )

                            // Text overlay: battery data only.
                            // Status indicators (state icon, lock, sentry, temps) are drawn
                            // directly into the background bitmap to match dashboard layout.
                            val batteryLevel = prefs[BATTERY_LEVEL_KEY] ?: 0
                            val isCharging = prefs[IS_CHARGING_KEY] ?: false
                            val isDcCharging = prefs[IS_DC_CHARGING_KEY] ?: false
                            val carName = prefs[CAR_NAME_KEY] ?: ""
                            val ratedRange = prefs[RATED_RANGE_KEY]?.takeIf { it >= 0f }
                            val chargeLimit = prefs[CHARGE_LIMIT_KEY]?.takeIf { it >= 0 }
                            val chargeEnergyAdded = prefs[CHARGE_ENERGY_ADDED_KEY]?.takeIf { it >= 0f }
                            val timeToFull = prefs[TIME_TO_FULL_KEY]?.takeIf { it >= 0f }
                            val chargerVoltage = prefs[CHARGER_VOLTAGE_KEY]?.takeIf { it >= 0 }
                            val chargerCurrent = prefs[CHARGER_CURRENT_KEY]?.takeIf { it >= 0 }
                            val acPhases = prefs[AC_PHASES_KEY]?.takeIf { it >= 0 }

                            Column(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Spacer(modifier = GlanceModifier.defaultWeight())

                                // Car name (small label above battery %)
                                if (carName.isNotEmpty()) {
                                    Text(
                                        text = carName,
                                        style = TextStyle(
                                            color = ColorProvider(Color.White.copy(alpha = 0.65f)),
                                            fontSize = 10.sp
                                        )
                                    )
                                }

                                // Battery level + AC/DC badge + range + charge limit
                                val batteryColor = when {
                                    batteryLevel < 20 -> Color(0xFFEF5350)
                                    batteryLevel < 40 -> Color(0xFFFF9800)
                                    else -> Color.White
                                }
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$batteryLevel%",
                                        style = TextStyle(
                                            color = ColorProvider(batteryColor),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    if (isCharging) {
                                        Text(
                                            text = if (isDcCharging) "  DC" else "  AC",
                                            style = TextStyle(
                                                color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    val rangeAndLimit = buildList<String> {
                                        if (ratedRange != null) add("${ratedRange.roundToInt()} km")
                                        if (chargeLimit != null) add("Limit: $chargeLimit%")
                                    }.joinToString("  ")
                                    if (rangeAndLimit.isNotEmpty()) {
                                        Text(
                                            text = rangeAndLimit,
                                            style = TextStyle(
                                                color = ColorProvider(Color.White.copy(alpha = 0.85f)),
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                // Charging details row: voltage / current / phases on left,
                                // energy added + time-to-full on right
                                if (isCharging) {
                                    val leftPart = buildString {
                                        if (chargerVoltage != null) append("${chargerVoltage}V")
                                        if (chargerCurrent != null) append(" ${chargerCurrent}A")
                                        if (!isDcCharging && acPhases != null) append(" ${acPhases}φ")
                                    }.trim()
                                    val rightPart = buildString {
                                        if (chargeEnergyAdded != null) append("+%.1f kWh".format(chargeEnergyAdded))
                                        if (timeToFull != null) {
                                            val h = timeToFull.toInt()
                                            val m = ((timeToFull - h) * 60).roundToInt()
                                            append(if (h > 0) " ${h}h ${m}m" else " ${m}m")
                                        }
                                    }.trim()
                                    val chargingText = listOf(leftPart, rightPart)
                                        .filter { it.isNotEmpty() }
                                        .joinToString("  ")
                                    if (chargingText.isNotEmpty()) {
                                        Text(
                                            text = chargingText,
                                            style = TextStyle(
                                                color = ColorProvider(Color.White.copy(alpha = 0.9f)),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Persists all [CarWidgetDisplayData] fields to Glance preferences and triggers
     * a redraw. This is the only way to get real data into the widget.
     */
    suspend fun updateWidget(context: Context, glanceId: GlanceId, data: CarWidgetDisplayData) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[CAR_ID_KEY] = data.carId
                this[HAS_DATA_KEY] = true
                this[CAR_NAME_KEY] = data.carName
                data.exteriorColor?.let { this[EXTERIOR_COLOR_KEY] = it }
                data.model?.let { this[MODEL_KEY] = it }
                data.trimBadging?.let { this[TRIM_BADGING_KEY] = it }
                data.wheelType?.let { this[WHEEL_TYPE_KEY] = it }
                data.state?.let { this[STATE_KEY] = it }
                this[IS_LOCKED_KEY] = data.isLocked
                this[SENTRY_MODE_KEY] = data.sentryModeActive
                this[PLUGGED_IN_KEY] = data.pluggedIn
                this[OUTSIDE_TEMP_KEY] = data.outsideTemp?.toFloat() ?: Float.NaN
                this[INSIDE_TEMP_KEY] = data.insideTemp?.toFloat() ?: Float.NaN
                this[IS_CLIMATE_ON_KEY] = data.isClimateOn
                this[BATTERY_LEVEL_KEY] = data.batteryLevel
                this[RATED_RANGE_KEY] = data.ratedBatteryRangeKm?.toFloat() ?: -1f
                this[CHARGE_LIMIT_KEY] = data.chargeLimitSoc ?: -1
                this[IS_CHARGING_KEY] = data.isCharging
                this[IS_DC_CHARGING_KEY] = data.isDcCharging
                this[CHARGER_POWER_KEY] = data.chargerPower ?: -1
                this[CHARGE_ENERGY_ADDED_KEY] = data.chargeEnergyAdded?.toFloat() ?: -1f
                this[TIME_TO_FULL_KEY] = data.timeToFullCharge?.toFloat() ?: -1f
                this[CHARGER_VOLTAGE_KEY] = data.chargerVoltage ?: -1
                this[CHARGER_CURRENT_KEY] = data.chargerActualCurrent ?: -1
                this[AC_PHASES_KEY] = data.acPhases ?: -1
            }
        }
        update(context, glanceId)
    }

    // -------------------------------------------------------------------------
    // Background bitmap generation
    // -------------------------------------------------------------------------

    /**
     * Generates the full background bitmap at the exact widget pixel size.
     * Layers (bottom to top):
     *  1. Palette surface color
     *  2. Car glow (if charging)
     *  3. Dimmed car image (aspect-ratio correct)
     *  4. Gradient scrim (dark at top and bottom, transparent in the middle)
     *  5. Status bar icons (state icon + lock + sentry dot + plug | temps)
     *  6. Progress bar at the very bottom
     */
    private fun buildBackgroundBitmap(
        context: Context,
        prefs: Preferences,
        width: Int,
        height: Int,
        density: Float = 2f
    ): Bitmap {
        val exteriorColor = prefs[EXTERIOR_COLOR_KEY]
        val model = prefs[MODEL_KEY]
        val trimBadging = prefs[TRIM_BADGING_KEY]
        val wheelType = prefs[WHEEL_TYPE_KEY]
        val state = prefs[STATE_KEY]
        val isLocked = prefs[IS_LOCKED_KEY] ?: false
        val sentryMode = prefs[SENTRY_MODE_KEY] ?: false
        val pluggedIn = prefs[PLUGGED_IN_KEY] ?: false
        val isClimateOn = prefs[IS_CLIMATE_ON_KEY] ?: false
        val isCharging = prefs[IS_CHARGING_KEY] ?: false
        val isDcCharging = prefs[IS_DC_CHARGING_KEY] ?: false
        val batteryLevel = prefs[BATTERY_LEVEL_KEY] ?: 0
        val chargeLimit = prefs[CHARGE_LIMIT_KEY]?.takeIf { it >= 0 }
        val outsideTemp = prefs[OUTSIDE_TEMP_KEY]?.takeIf { !it.isNaN() }
        val insideTemp = prefs[INSIDE_TEMP_KEY]?.takeIf { !it.isNaN() }

        val palette = CarColorPalettes.forExteriorColor(exteriorColor, darkTheme = true)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Solid background
        canvas.drawColor(colorToAndroidArgb(palette.surface))

        // Status bar: icon height matches the dashboard's compact icon size (16dp).
        // Top pad of 8dp and horizontal pad of 12dp keep all content well inside
        // the 16dp corner radius used by cornerRadius() above, preventing clipping.
        val iconSzPx = 16f * density
        val sbTopPadPx = 8f * density
        val sbHorzPadPx = 12f * density
        val statusBarH = iconSzPx + sbTopPadPx * 2f
        val carAreaTop = statusBarH
        val carAreaH = height.toFloat() - carAreaTop

        // 2 & 3. Car image (glow behind, dimmed car on top)
        val carBitmap = loadCarBitmap(context, model, exteriorColor, wheelType, trimBadging)
        if (carBitmap != null) {
            // Scale to fit the car area while preserving aspect ratio
            val carAspect = carBitmap.width.toFloat() / carBitmap.height.toFloat()
            val areaAspect = width.toFloat() / carAreaH
            val scaledW: Int
            val scaledH: Int
            if (carAspect >= areaAspect) {
                scaledW = (width * 0.95f).roundToInt()
                scaledH = (scaledW / carAspect).roundToInt().coerceAtLeast(1)
            } else {
                scaledH = (carAreaH * 0.85f).roundToInt()
                scaledW = (scaledH * carAspect).roundToInt().coerceAtLeast(1)
            }

            val carLeft = (width - scaledW) / 2f
            val carTop = carAreaTop + (carAreaH - scaledH) * 0.35f

            // Glow first (behind the car)
            if (isCharging) {
                val glowColor = if (isDcCharging) palette.dcColor else palette.acColor
                val glowBitmap = GlowBitmapRenderer.createGlowBitmap(carBitmap, glowColor, 30f)
                val glowScaledW = (scaledW * 1.4f).roundToInt().coerceAtLeast(1)
                val glowScaledH = (scaledH * 1.4f).roundToInt().coerceAtLeast(1)
                val glowScaled = Bitmap.createScaledBitmap(glowBitmap, glowScaledW, glowScaledH, true)
                canvas.drawBitmap(
                    glowScaled,
                    (width - glowScaled.width) / 2f,
                    carTop - (glowScaled.height - scaledH) / 2f,
                    null
                )
                glowScaled.recycle()
                glowBitmap.recycle()
            }

            // Dimmed car on top
            val dimmed = GlowBitmapRenderer.createDimmedCarBitmap(carBitmap, 0.35f)
            val scaled = Bitmap.createScaledBitmap(dimmed, scaledW, scaledH, true)
            canvas.drawBitmap(scaled, carLeft, carTop, null)
            scaled.recycle()
            dimmed.recycle()
            carBitmap.recycle()
        }

        // 4. Gradient scrim: dark at top (status bar) + dark at bottom (text area)
        val scrimPaint = Paint()
        scrimPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                android.graphics.Color.argb(210, 0, 0, 0),
                android.graphics.Color.argb(0, 0, 0, 0),
                android.graphics.Color.argb(210, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        // 5. Status bar icons (drawn after scrim so they are visible)
        drawStatusBar(
            canvas, sbTopPadPx, sbHorzPadPx, iconSzPx, width,
            state, isLocked, sentryMode, pluggedIn,
            isClimateOn, outsideTemp, insideTemp, palette
        )

        // 6. Progress bar at the very bottom
        val barH = (height * 0.03f).coerceAtLeast(4f).coerceAtMost(8f)
        val barTop = height - barH
        val barRadius = barH / 2f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Track
        barPaint.color = android.graphics.Color.argb(80, 255, 255, 255)
        canvas.drawRoundRect(RectF(0f, barTop, width.toFloat(), height.toFloat()), barRadius, barRadius, barPaint)

        // Charge limit zone (dimmed colour from current level to limit)
        if (chargeLimit != null && chargeLimit > batteryLevel) {
            val dimColor = if (isDcCharging) palette.dcColor else if (isCharging) palette.acColor else palette.accent
            barPaint.color = android.graphics.Color.argb(
                60,
                (dimColor.red * 255).toInt(),
                (dimColor.green * 255).toInt(),
                (dimColor.blue * 255).toInt()
            )
            canvas.drawRect(
                width * batteryLevel / 100f, barTop,
                width * chargeLimit / 100f, height.toFloat(),
                barPaint
            )
        }

        // Battery fill
        val fillColor = when {
            isCharging && isDcCharging -> palette.dcColor
            isCharging -> palette.acColor
            batteryLevel < 20 -> Color(0xFFEF5350)
            batteryLevel < 40 -> Color(0xFFFF9800)
            else -> palette.accent
        }
        barPaint.color = android.graphics.Color.argb(
            230,
            (fillColor.red * 255).toInt(),
            (fillColor.green * 255).toInt(),
            (fillColor.blue * 255).toInt()
        )
        val fillW = width * batteryLevel / 100f
        if (fillW > 0) {
            canvas.drawRoundRect(RectF(0f, barTop, fillW, height.toFloat()), barRadius, barRadius, barPaint)
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Status bar: drawn into the bitmap to match the dashboard layout exactly.
    // LEFT side:  state icon → lock icon → sentry dot (if active) → plug (if plugged, not charging)
    // RIGHT side: "Ext: XX°   Int: XX°" (green + bold when climate on)
    // -------------------------------------------------------------------------

    private fun drawStatusBar(
        canvas: Canvas,
        topPad: Float,
        horzPad: Float,
        iconSz: Float,
        bitmapWidth: Int,
        state: String?,
        isLocked: Boolean,
        sentryMode: Boolean,
        pluggedIn: Boolean,
        isClimateOn: Boolean,
        outsideTemp: Float?,
        insideTemp: Float?,
        palette: CarColorPalette
    ) {
        val stateLower = state?.lowercase()
        val isCharging = stateLower == "charging"
        val isDriving = stateLower == "driving"
        val isAsleep = stateLower in listOf("asleep", "suspended")
        val isAwake = stateLower in listOf("online", "charging", "driving", "updating")

        // State icon colour: StatusSuccess (green) if awake, onSurfaceVariant (grey) otherwise
        val stateColor = if (isAwake) ANDROID_STATUS_SUCCESS
        else colorToAndroidArgb(palette.onSurfaceVariant.copy(alpha = 0.8f))

        // onSurfaceVariant as an Android int (for lock + plug)
        val variantColor = colorToAndroidArgb(palette.onSurfaceVariant.copy(alpha = 0.85f))

        // Lock colour: grey when locked, light red when unlocked
        val lockColor = if (isLocked) variantColor else ANDROID_STATUS_ERROR_DIM

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cy = topPad + iconSz / 2f    // vertical centre of the status bar row
        var cursorX = horzPad

        // --- State icon ---
        iconPaint.color = stateColor
        when {
            isCharging -> drawLightningBolt(canvas, cursorX + iconSz / 2f, cy, iconSz, iconPaint)
            isAsleep -> drawCrescent(canvas, cursorX + iconSz / 2f, cy, iconSz / 2f - 1f, iconPaint)
            isDriving -> drawSteeringWheel(canvas, cursorX + iconSz / 2f, cy, iconSz / 2f - 1f, iconPaint)
            else -> drawPowerSymbol(canvas, cursorX + iconSz / 2f, cy, iconSz / 2f - 1f, iconPaint)
        }
        val iconGap = iconSz * 0.5f   // gap between icons, ~8dp
        cursorX += iconSz + iconGap

        // --- Lock icon ---
        iconPaint.color = lockColor
        drawPadlock(canvas, cursorX + iconSz / 2f, cy, iconSz * 0.44f, isLocked, iconPaint)
        cursorX += iconSz + iconGap

        // --- Sentry dot (12dp-equivalent red circle, same as dashboard) ---
        if (sentryMode) {
            iconPaint.color = ANDROID_STATUS_ERROR
            iconPaint.style = Paint.Style.FILL
            val dotR = iconSz * 0.25f
            canvas.drawCircle(cursorX + dotR, cy, dotR, iconPaint)
            cursorX += dotR * 2f + iconGap
        }

        // --- Plug icon (shown when plugged in but not currently charging) ---
        if (pluggedIn && !isCharging) {
            iconPaint.color = variantColor
            drawPlugIcon(canvas, cursorX + iconSz / 2f, cy, iconSz * 0.5f, iconPaint)
        }

        // --- Temperatures (RIGHT side, right-aligned) ---
        val tempParts = buildList<String> {
            if (outsideTemp != null) add("Ext: %.0f°".format(outsideTemp))
            if (insideTemp != null) add("Int: %.0f°".format(insideTemp))
        }
        if (tempParts.isNotEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            textPaint.textSize = iconSz * 0.72f
            textPaint.textAlign = Paint.Align.RIGHT
            if (isClimateOn) {
                textPaint.color = ANDROID_STATUS_SUCCESS
                textPaint.typeface = Typeface.DEFAULT_BOLD
            } else {
                textPaint.color = variantColor
                textPaint.typeface = Typeface.DEFAULT
            }
            // Baseline: centre the text vertically within the icon row
            val textBaseline = cy + textPaint.textSize * 0.36f
            canvas.drawText(
                tempParts.joinToString("  "),
                bitmapWidth - horzPad,
                textBaseline,
                textPaint
            )
        }
    }

    // -------------------------------------------------------------------------
    // Icon drawing helpers — each draws within an iconSz × iconSz box
    // -------------------------------------------------------------------------

    /** ElectricBolt — classic lightning-bolt polygon. */
    private fun drawLightningBolt(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val w = size * 0.48f
        val h = size * 0.78f
        val path = Path()
        path.moveTo(cx + w * 0.18f, cy - h / 2f)       // top-right
        path.lineTo(cx - w * 0.50f, cy + h * 0.06f)    // left-middle
        path.lineTo(cx + w * 0.08f, cy + h * 0.06f)    // center-middle
        path.lineTo(cx - w * 0.18f, cy + h / 2f)       // bottom-left
        path.lineTo(cx + w * 0.50f, cy - h * 0.06f)    // right-middle
        path.lineTo(cx - w * 0.08f, cy - h * 0.06f)    // center-upper
        path.close()
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    /** Bedtime — crescent moon via EVEN_ODD path. */
    private fun drawCrescent(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        path.addCircle(cx, cy, r, Path.Direction.CW)
        // Overlapping circle creates the crescent cutout
        path.addCircle(cx + r * 0.38f, cy - r * 0.18f, r * 0.70f, Path.Direction.CW)
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    /** SteeringWheel — outer rim + hub + three equally-spaced spokes (Y-shape). */
    private fun drawSteeringWheel(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.20f
        paint.strokeCap = Paint.Cap.ROUND

        // Outer rim
        canvas.drawCircle(cx, cy, r, paint)

        // Hub
        val hubR = r * 0.28f
        canvas.drawCircle(cx, cy, hubR, paint)

        // Three spokes at 30°, 150°, 270° (canvas clockwise from right)
        // → lower-right, lower-left, top — forming a Y
        for (angleDeg in listOf(30.0, 150.0, 270.0)) {
            val rad = angleDeg * PI / 180.0
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            canvas.drawLine(
                cx + hubR * cosA, cy + hubR * sinA,
                cx + r * cosA,    cy + r * sinA,
                paint
            )
        }
    }

    /** PowerSettingsNew — circle arc with gap at top + vertical line through the gap. */
    private fun drawPowerSymbol(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        // Use a smaller effective radius so the symbol sits comfortably in its cell
        val er = r * 0.82f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = er * 0.20f
        paint.strokeCap = Paint.Cap.ROUND
        // 60° gap centred at the top (270° in Android canvas coordinates):
        // arc from 300° sweeping 300° clockwise ends at 240°, leaving the 240°–300° gap at top.
        canvas.drawArc(
            RectF(cx - er, cy - er, cx + er, cy + er),
            300f, 300f, false, paint
        )
        // Vertical line from top through the gap down to ~35% radius
        canvas.drawLine(cx, cy - er * 0.98f, cx, cy - er * 0.30f, paint)
    }

    /**
     * Lock / LockOpen padlock.
     * Body: filled rounded rect.
     * Shackle: U-shaped arc + two sides.
     *   Locked  → right side connects to body.
     *   Unlocked → right side is raised/open.
     */
    private fun drawPadlock(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        isLocked: Boolean,
        paint: Paint
    ) {
        // Body (lower portion of the icon)
        val bodyW = r * 1.5f
        val bodyH = r * 1.05f
        val bodyTop = cy - r * 0.12f
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            RectF(cx - bodyW / 2f, bodyTop, cx + bodyW / 2f, bodyTop + bodyH),
            r * 0.18f, r * 0.18f, paint
        )

        // Keyhole (dark circle inside body)
        val savedColor = paint.color
        paint.color = android.graphics.Color.argb(100, 0, 0, 0)
        canvas.drawCircle(cx, bodyTop + bodyH * 0.42f, r * 0.17f, paint)
        paint.color = savedColor

        // Shackle (U-shape above the body)
        val shR = r * 0.38f          // radius of the shackle arc
        val arcCy = bodyTop - shR    // centre of the arc's circle
        val arcRect = RectF(cx - shR, arcCy - shR, cx + shR, arcCy + shR)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.30f
        paint.strokeCap = Paint.Cap.ROUND

        // Top arc: from 180° sweep 180° → draws the upper semicircle (arch at top)
        canvas.drawArc(arcRect, 180f, 180f, false, paint)
        // Left side: always connects down to the body
        canvas.drawLine(cx - shR, arcCy, cx - shR, bodyTop, paint)

        if (isLocked) {
            // Right side connects to body
            canvas.drawLine(cx + shR, arcCy, cx + shR, bodyTop, paint)
        } else {
            // Right side is raised (open shackle) — extends further up
            canvas.drawLine(cx + shR, arcCy, cx + shR, arcCy - shR * 0.85f, paint)
        }
    }

    /** Power plug — two prongs at top + rounded body. */
    private fun drawPlugIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val bodyW = r * 1.1f
        val bodyH = r * 1.15f
        val bodyTop = cy - bodyH * 0.38f
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            RectF(cx - bodyW / 2f, bodyTop, cx + bodyW / 2f, bodyTop + bodyH),
            r * 0.2f, r * 0.2f, paint
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.22f
        paint.strokeCap = Paint.Cap.ROUND
        val prongX1 = cx - bodyW * 0.25f
        val prongX2 = cx + bodyW * 0.25f
        val prongTop = bodyTop - r * 0.5f
        canvas.drawLine(prongX1, bodyTop, prongX1, prongTop, paint)
        canvas.drawLine(prongX2, bodyTop, prongX2, prongTop, paint)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun loadCarBitmap(
        context: Context,
        model: String?,
        exteriorColor: String?,
        wheelType: String?,
        trimBadging: String?
    ): Bitmap? {
        val assetPath = CarImageResolver.getAssetPath(model, exteriorColor, wheelType, trimBadging)
        return try {
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
        } catch (_: IOException) {
            try {
                val fallback = CarImageResolver.getDefaultAssetPath(model)
                context.assets.open(fallback).use { BitmapFactory.decodeStream(it) }
            } catch (_: IOException) {
                null
            }
        }
    }

    /** Converts a Compose [Color] to an Android packed ARGB int. */
    private fun colorToAndroidArgb(color: Color): Int = android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )
}
