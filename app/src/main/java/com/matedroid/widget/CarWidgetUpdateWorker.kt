package com.matedroid.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.matedroid.data.repository.ApiResult
import com.matedroid.data.repository.TeslamateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that fetches car status and updates all active home screen widgets.
 * Runs every 15 minutes (WorkManager minimum for periodic work).
 */
@HiltWorker
class CarWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val teslamateRepository: TeslamateRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CarWidgetUpdateWorker"
        const val PERIODIC_WORK_NAME = "car_widget_update_periodic"

        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<CarWidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.d(TAG, "Scheduled periodic widget update (15 min)")
        }

        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            Log.d(TAG, "Cancelled widget update work")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting widget update")

        val manager = GlanceAppWidgetManager(appContext)
        val glanceIds = manager.getGlanceIds(CarWidget::class.java)

        if (glanceIds.isEmpty()) {
            Log.d(TAG, "No active widgets, skipping update")
            return Result.success()
        }

        // Fetch all cars once to avoid redundant API calls
        val carsResult = teslamateRepository.getCars()
        val cars = when (carsResult) {
            is ApiResult.Success -> carsResult.data
            is ApiResult.Error -> {
                Log.e(TAG, "Failed to fetch cars: ${carsResult.message}")
                return Result.retry()
            }
        }

        for (glanceId in glanceIds) {
            try {
                val prefs = getAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId)
                val carId = prefs[CarWidget.CAR_ID_KEY] ?: continue

                val car = cars.find { it.carId == carId } ?: continue
                val statusResult = teslamateRepository.getCarStatus(carId)
                val status = when (statusResult) {
                    is ApiResult.Success -> statusResult.data.status
                    is ApiResult.Error -> {
                        Log.e(TAG, "Failed to fetch status for car $carId: ${statusResult.message}")
                        continue
                    }
                }

                val displayData = CarWidgetDisplayData.from(car, status)
                CarWidget().updateWidget(appContext, glanceId, displayData)
                Log.d(TAG, "Updated widget for car $carId (${car.displayName})")

            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget $glanceId", e)
            }
        }

        return Result.success()
    }
}
