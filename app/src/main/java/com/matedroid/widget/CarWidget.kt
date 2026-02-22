package com.matedroid.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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
 * The widget requires a [CAR_ID_KEY] to be stored in Glance preferences (set by
 * [CarWidgetConfigActivity]). On each update, it renders a summary of the
 * battery card fields defined in [CarWidgetDisplayData].
 */
class CarWidget : GlanceAppWidget() {

    companion object {
        val CAR_ID_KEY = intPreferencesKey("car_id")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val carId = prefs[CAR_ID_KEY]

            GlanceTheme {
                if (carId == null) {
                    // Not yet configured — show setup hint
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(Color(0xFF1E2530)))
                            .padding(12.dp)
                            .clickable(actionStartActivity<MainActivity>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(com.matedroid.R.string.widget_error_configure),
                            style = TextStyle(color = ColorProvider(Color.White))
                        )
                    }
                } else {
                    // Placeholder until first data fetch populates real content
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(Color(0xFF1E2530)))
                            .padding(12.dp)
                            .clickable(actionStartActivity<MainActivity>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(com.matedroid.R.string.widget_loading),
                            style = TextStyle(
                                color = ColorProvider(Color.White.copy(alpha = 0.7f))
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Persists the car ID and triggers a visual refresh of this widget instance.
     * Called by [CarWidgetUpdateWorker] after fetching fresh data.
     */
    suspend fun updateWidget(
        context: Context,
        glanceId: GlanceId,
        data: CarWidgetDisplayData
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[CAR_ID_KEY] = data.carId
            }
        }
        update(context, glanceId)
    }

    // ----- Bitmap helpers (used by preview / future RemoteViews content) -----

    internal fun loadCarBitmap(context: Context, data: CarWidgetDisplayData): Bitmap? {
        val assetPath = CarImageResolver.getAssetPath(
            model = data.model,
            exteriorColor = data.exteriorColor,
            wheelType = data.wheelType,
            trimBadging = data.trimBadging
        )
        return try {
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
        } catch (_: IOException) {
            try {
                val fallback = CarImageResolver.getDefaultAssetPath(data.model)
                context.assets.open(fallback).use { BitmapFactory.decodeStream(it) }
            } catch (_: IOException) {
                null
            }
        }
    }

    internal fun buildWidgetBitmap(
        context: Context,
        data: CarWidgetDisplayData,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val palette = CarColorPalettes.forExteriorColor(data.exteriorColor, darkTheme = true)
        val carBitmap = loadCarBitmap(context, data)

        val result = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        canvas.drawColor(
            android.graphics.Color.argb(
                255,
                (palette.surface.red * 255).toInt(),
                (palette.surface.green * 255).toInt(),
                (palette.surface.blue * 255).toInt()
            )
        )

        if (carBitmap != null) {
            val dimmed = GlowBitmapRenderer.createDimmedCarBitmap(carBitmap)
            val scaleFactor = CarImageResolver.getScaleFactor(
                data.model, data.exteriorColor, data.wheelType, data.trimBadging
            )
            val scaledW = (dimmed.width * scaleFactor).roundToInt().coerceAtMost(widthPx)
            val aspectRatio = dimmed.height.toFloat() / dimmed.width
            val scaledH = (scaledW * aspectRatio).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(
                dimmed, scaledW.coerceAtLeast(1), scaledH, true
            )
            val left = (widthPx - scaled.width) / 2f
            val top = (heightPx - scaled.height) * 0.3f
            canvas.drawBitmap(scaled, left, top, null)

            if (data.isCharging) {
                val glowColor = if (data.isDcCharging) palette.dcColor else palette.acColor
                val glowBitmap = GlowBitmapRenderer.createGlowBitmap(carBitmap, glowColor, 40f)
                val glowLeft = left - (glowBitmap.width - scaled.width) / 2f
                val glowTop = top - (glowBitmap.height - scaled.height) / 2f
                canvas.drawBitmap(glowBitmap, glowLeft, glowTop, null)
                glowBitmap.recycle()
            }

            scaled.recycle()
            dimmed.recycle()
            carBitmap.recycle()
        }

        return result
    }
}
