package com.matedroid.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.matedroid.R
import com.matedroid.data.repository.ApiResult
import com.matedroid.data.repository.TeslamateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Background worker for syncing stats data from TeslamateApi.
 *
 * Runs as a foreground service to prevent being killed when app is in background.
 * - Phase 1: Sync summaries (fast, for Quick Stats)
 * - Phase 2: Sync details (slow, for Deep Stats)
 */
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val teslamateRepository: TeslamateRepository,
    private val syncRepository: SyncRepository,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "DataSyncWorker"
        const val WORK_NAME = "data_sync_work"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sync_channel"
    }

    /**
     * Required for expedited work - provides foreground info for older API levels.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("Syncing data...")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting data sync worker")

        // Get list of cars
        val carsResult = teslamateRepository.getCars()
        val cars = when (carsResult) {
            is ApiResult.Success -> carsResult.data
            is ApiResult.Error -> {
                Log.e(TAG, "Failed to fetch cars: ${carsResult.message}")
                return Result.retry()
            }
        }

        if (cars.isEmpty()) {
            Log.d(TAG, "No cars found, nothing to sync")
            return Result.success()
        }

        Log.d(TAG, "Found ${cars.size} cars to sync")

        // Sync all cars in parallel
        val results = coroutineScope {
            cars.map { car ->
                async {
                    try {
                        syncRepository.syncCar(car.carId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing car ${car.carId}", e)
                        syncManager.markSyncError(car.carId, e.message ?: "Unknown error")
                        false
                    }
                }
            }.awaitAll()
        }

        val allSuccess = results.all { it }
        Log.d(TAG, "Sync complete. All success: $allSuccess")

        return if (allSuccess) Result.success() else Result.success() // Still success to not retry indefinitely
    }

    /**
     * Create foreground info for the notification.
     */
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val context = applicationContext

        // Create notification channel (required for Android 8.0+)
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MateDroid Sync")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Data Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background sync for stats data"
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
