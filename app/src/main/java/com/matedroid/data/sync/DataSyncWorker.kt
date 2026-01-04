package com.matedroid.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
 * Runs on app launch and syncs all cars in parallel:
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
}
