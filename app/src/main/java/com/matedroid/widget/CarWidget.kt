package com.matedroid.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import com.matedroid.ui.theme.CarColorPalettes
import com.matedroid.ui.util.GlowBitmapRenderer
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Home screen widget displaying real-time battery info for a configured car.
 *
 * All display data is persisted in Glance preferences so that [provideGlance]
 * can render real content without needing to inject [TeslamateRepository].
 * [updateWidget] writes every field from [CarWidgetDisplayData] into preferences
 * and then calls [update] to trigger a redraw.
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

        // Background bitmap resolution — small enough to fit in RemoteViews parcel
        private const val BG_W = 400
        private const val BG_H = 200
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read persisted state before entering the composable so we can do
        // bitmap work (blocking) here rather than inside the Glance content block.
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val carId = prefs[CAR_ID_KEY]
        val hasData = prefs[HAS_DATA_KEY] ?: false

        val bgBitmap: Bitmap? = if (hasData && carId != null) {
            buildBackgroundBitmap(context, prefs)
        } else null

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    when {
                        carId == null -> {
                            // Widget placed but not yet configured (shouldn't happen with auto-select)
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(Color(0xFF1E2530)))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = context.getString(com.matedroid.R.string.widget_error_configure),
                                    style = TextStyle(color = ColorProvider(Color.White))
                                )
                            }
                        }

                        !hasData -> {
                            // carId set but first data fetch not yet complete
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(ColorProvider(Color(0xFF1E2530)))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = context.getString(com.matedroid.R.string.widget_loading),
                                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)))
                                )
                            }
                        }

                        else -> {
                            // --- Real widget content ---
                            val batteryLevel = prefs[BATTERY_LEVEL_KEY] ?: 0
                            val isCharging = prefs[IS_CHARGING_KEY] ?: false
                            val isDcCharging = prefs[IS_DC_CHARGING_KEY] ?: false
                            val isLocked = prefs[IS_LOCKED_KEY] ?: false
                            val sentryMode = prefs[SENTRY_MODE_KEY] ?: false
                            val isClimateOn = prefs[IS_CLIMATE_ON_KEY] ?: false
                            val carName = prefs[CAR_NAME_KEY] ?: ""
                            val state = prefs[STATE_KEY]
                            val outsideTemp = prefs[OUTSIDE_TEMP_KEY]?.takeIf { !it.isNaN() }
                            val insideTemp = prefs[INSIDE_TEMP_KEY]?.takeIf { !it.isNaN() }
                            val ratedRange = prefs[RATED_RANGE_KEY]?.takeIf { it >= 0f }
                            val chargeLimit = prefs[CHARGE_LIMIT_KEY]?.takeIf { it >= 0 }
                            val chargerPower = prefs[CHARGER_POWER_KEY]?.takeIf { it >= 0 }
                            val chargeEnergyAdded = prefs[CHARGE_ENERGY_ADDED_KEY]?.takeIf { it >= 0f }
                            val timeToFull = prefs[TIME_TO_FULL_KEY]?.takeIf { it >= 0f }
                            val chargerVoltage = prefs[CHARGER_VOLTAGE_KEY]?.takeIf { it >= 0 }
                            val chargerCurrent = prefs[CHARGER_CURRENT_KEY]?.takeIf { it >= 0 }
                            val acPhases = prefs[AC_PHASES_KEY]?.takeIf { it >= 0 }

                            // Background: pre-generated car image bitmap
                            if (bgBitmap != null) {
                                Image(
                                    provider = ImageProvider(bgBitmap),
                                    contentDescription = null,
                                    modifier = GlanceModifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            } else {
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxSize()
                                        .background(ColorProvider(Color(0xFF1E2530)))
                                ) {}
                            }

                            // Text overlay
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                // Top: car name + status icons
                                Row(modifier = GlanceModifier.fillMaxWidth()) {
                                    Text(
                                        text = carName,
                                        style = TextStyle(
                                            color = ColorProvider(Color.White),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    val icons = buildString {
                                        if (isLocked) append("🔒")
                                        if (sentryMode) append(" 🔴")
                                        if (isClimateOn) append(" ❄️")
                                    }.trim()
                                    if (icons.isNotEmpty()) {
                                        Text(text = icons, style = TextStyle(fontSize = 12.sp))
                                    }
                                }

                                // Temperatures (if available)
                                if (outsideTemp != null || insideTemp != null) {
                                    val temps = buildList<String> {
                                        if (outsideTemp != null) add(
                                            "${context.getString(com.matedroid.R.string.temp_ext_label)} %.0f°".format(outsideTemp)
                                        )
                                        if (insideTemp != null) add(
                                            "${context.getString(com.matedroid.R.string.temp_int_label)} %.0f°".format(insideTemp)
                                        )
                                    }
                                    Text(
                                        text = temps.joinToString("  "),
                                        style = TextStyle(
                                            color = ColorProvider(Color.White.copy(alpha = 0.75f)),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Spacer(modifier = GlanceModifier.defaultWeight())

                                // Battery level + AC/DC badge + range + limit
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

                                // Charging details or state label
                                if (isCharging) {
                                    val parts = buildList<String> {
                                        if (chargerPower != null) add("$chargerPower kW")
                                        val elec = buildString {
                                            if (chargerVoltage != null) append("${chargerVoltage}V")
                                            if (chargerCurrent != null) append(" ${chargerCurrent}A")
                                            if (acPhases != null) append(" ${acPhases}φ")
                                        }.trim()
                                        if (elec.isNotEmpty()) add(elec)
                                        if (chargeEnergyAdded != null) add("+%.1f kWh".format(chargeEnergyAdded))
                                        if (timeToFull != null) {
                                            val h = timeToFull.toInt()
                                            val m = ((timeToFull - h) * 60).roundToInt()
                                            add(if (h > 0) "${h}h ${m}m" else "${m}m")
                                        }
                                    }
                                    if (parts.isNotEmpty()) {
                                        Text(
                                            text = parts.joinToString("  "),
                                            style = TextStyle(
                                                color = ColorProvider(Color.White.copy(alpha = 0.9f)),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                } else if (!state.isNullOrEmpty() && state.lowercase() != "online") {
                                    Text(
                                        text = state.replaceFirstChar { it.uppercase() },
                                        style = TextStyle(
                                            color = ColorProvider(Color.White.copy(alpha = 0.65f)),
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

    /**
     * Generates the background bitmap: palette surface color + dimmed car image
     * + glow if charging + gradient scrim + progress bar.
     */
    private fun buildBackgroundBitmap(context: Context, prefs: Preferences): Bitmap {
        val exteriorColor = prefs[EXTERIOR_COLOR_KEY]
        val model = prefs[MODEL_KEY]
        val trimBadging = prefs[TRIM_BADGING_KEY]
        val wheelType = prefs[WHEEL_TYPE_KEY]
        val isCharging = prefs[IS_CHARGING_KEY] ?: false
        val isDcCharging = prefs[IS_DC_CHARGING_KEY] ?: false
        val batteryLevel = prefs[BATTERY_LEVEL_KEY] ?: 0
        val chargeLimit = prefs[CHARGE_LIMIT_KEY]?.takeIf { it >= 0 }

        val palette = CarColorPalettes.forExteriorColor(exteriorColor, darkTheme = true)

        val result = Bitmap.createBitmap(BG_W, BG_H, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        // Palette surface background
        canvas.drawColor(
            android.graphics.Color.argb(
                255,
                (palette.surface.red * 255).toInt(),
                (palette.surface.green * 255).toInt(),
                (palette.surface.blue * 255).toInt()
            )
        )

        // Car image (dimmed + optional glow)
        val carBitmap = loadCarBitmap(context, model, exteriorColor, wheelType, trimBadging)
        if (carBitmap != null) {
            val dimmed = GlowBitmapRenderer.createDimmedCarBitmap(carBitmap, 0.35f)
            val scaleFactor = CarImageResolver.getScaleFactor(model, exteriorColor, wheelType, trimBadging)
            val scaledW = (carBitmap.width * scaleFactor).roundToInt().coerceAtMost(BG_W)
            val aspect = carBitmap.height.toFloat() / carBitmap.width
            val scaledH = (scaledW * aspect).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(dimmed, scaledW.coerceAtLeast(1), scaledH, true)
            val left = (BG_W - scaled.width) / 2f
            val top = (BG_H - scaled.height) * 0.35f
            canvas.drawBitmap(scaled, left, top, null)

            if (isCharging) {
                val glowColor = if (isDcCharging) palette.dcColor else palette.acColor
                val glowBitmap = GlowBitmapRenderer.createGlowBitmap(carBitmap, glowColor, 30f)
                val glowLeft = left - (glowBitmap.width - scaled.width) / 2f
                val glowTop = top - (glowBitmap.height - scaled.height) / 2f
                canvas.drawBitmap(glowBitmap, glowLeft, glowTop, null)
                glowBitmap.recycle()
            }

            scaled.recycle()
            dimmed.recycle()
            carBitmap.recycle()
        }

        // Gradient scrim: darker at top (name) and bottom (data rows), transparent in the middle
        val scrimPaint = Paint()
        scrimPaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, BG_H.toFloat(),
            intArrayOf(
                android.graphics.Color.argb(180, 0, 0, 0),
                android.graphics.Color.argb(0, 0, 0, 0),
                android.graphics.Color.argb(160, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, BG_W.toFloat(), BG_H.toFloat(), scrimPaint)

        // Progress bar at the very bottom
        val barH = 6f
        val barTop = BG_H - barH
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Track
        barPaint.color = android.graphics.Color.argb(80, 255, 255, 255)
        canvas.drawRoundRect(RectF(0f, barTop, BG_W.toFloat(), BG_H.toFloat()), 3f, 3f, barPaint)

        // Charge limit zone (dimmed charge color from current level to limit)
        if (chargeLimit != null && chargeLimit > batteryLevel) {
            val limitX = BG_W * chargeLimit / 100f
            val dimColor = if (isDcCharging) palette.dcColor else if (isCharging) palette.acColor else palette.accent
            barPaint.color = android.graphics.Color.argb(
                60,
                (dimColor.red * 255).toInt(),
                (dimColor.green * 255).toInt(),
                (dimColor.blue * 255).toInt()
            )
            canvas.drawRect(BG_W * batteryLevel / 100f, barTop, limitX, BG_H.toFloat(), barPaint)
        }

        // Battery level fill
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
        val fillW = BG_W * batteryLevel / 100f
        if (fillW > 0) {
            canvas.drawRoundRect(RectF(0f, barTop, fillW, BG_H.toFloat()), 3f, 3f, barPaint)
        }

        return result
    }

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
}
