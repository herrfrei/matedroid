package com.matedroid.data.repository

import android.util.Log
import com.matedroid.data.api.TeslamateApi
import com.matedroid.data.api.models.BatteryHealth
import com.matedroid.data.api.models.CarData
import com.matedroid.data.api.models.CarStatus
import com.matedroid.data.api.models.ChargeData
import com.matedroid.data.api.models.Units
import com.matedroid.data.api.models.ChargeDetail
import com.matedroid.data.api.models.DriveData
import com.matedroid.data.api.models.DriveDetail
import com.matedroid.data.api.models.UpdateData
import com.matedroid.data.local.AppSettings
import com.matedroid.data.local.SettingsDataStore
import com.matedroid.di.TeslamateApiFactory
import kotlinx.coroutines.flow.first
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

data class CarStatusWithUnits(
    val status: CarStatus,
    val units: Units
)

/**
 * Represents exceptions that should trigger a fallback to the secondary server.
 * These are network-level errors where the server is unreachable, not application-level errors.
 */
private fun Throwable.isNetworkError(): Boolean {
    return this is SocketTimeoutException ||
            this is ConnectException ||
            this is UnknownHostException ||
            this is SSLException ||
            this is java.io.IOException && message?.contains("connection", ignoreCase = true) == true
}

@Singleton
class TeslamateRepository @Inject constructor(
    private val apiFactory: TeslamateApiFactory,
    private val settingsDataStore: SettingsDataStore,
    private val serverHealthMonitor: ServerHealthMonitor
) {
    companion object {
        private const val TAG = "TeslamateRepository"
    }

    private suspend fun getSettings(): AppSettings = settingsDataStore.settings.first()

    private fun getApiForUrl(url: String): TeslamateApi? {
        if (url.isBlank()) return null
        return apiFactory.create(url)
    }

    /**
     * Executes an API call with automatic fallback between primary and secondary servers.
     *
     * Uses the ServerHealthMonitor to determine which server to try first:
     * - If primary is known to be available, try it first
     * - If only secondary is available, try it first
     * - Falls back to the other server on network errors
     *
     * The fallback is triggered only for network-level errors (timeout, connection refused,
     * DNS failure, SSL errors). HTTP errors (4xx, 5xx) do NOT trigger fallback because
     * they indicate the server is reachable but returned an error.
     *
     * @param apiCall The API call to execute, given a TeslamateApi instance
     * @return The result of the API call
     */
    private suspend fun <T> executeWithFallback(
        apiCall: suspend (TeslamateApi) -> ApiResult<T>
    ): ApiResult<T> {
        val settings = getSettings()

        if (settings.serverUrl.isBlank()) {
            return ApiResult.Error("Server not configured")
        }

        // Determine server order based on health monitor
        val preference = serverHealthMonitor.serverPreference.value
        val (firstUrl, secondUrl) = when (preference) {
            ServerPreference.SECONDARY_ONLY -> {
                Log.d(TAG, "Using secondary server first (primary known unavailable)")
                settings.secondaryServerUrl to settings.serverUrl
            }
            else -> {
                // PRIMARY, UNKNOWN, or NONE - try primary first
                settings.serverUrl to settings.secondaryServerUrl
            }
        }

        // Try first server
        val firstApi = getApiForUrl(firstUrl)
            ?: return ApiResult.Error("Server not configured")

        val firstResult = try {
            apiCall(firstApi)
        } catch (e: Exception) {
            if (e.isNetworkError() && settings.hasSecondaryServer) {
                Log.d(TAG, "First server ($firstUrl) failed with network error, trying fallback: ${e.message}")
                null // Will try fallback
            } else {
                // Not a network error or no secondary server, return the error
                return when (e) {
                    is javax.net.ssl.SSLHandshakeException ->
                        ApiResult.Error("SSL certificate error. Enable 'Accept invalid certificates' for self-signed certs.")
                    else -> ApiResult.Error(e.message ?: "Connection failed")
                }
            }
        }

        // If first server succeeded or returned an HTTP error, return it
        if (firstResult != null) {
            // Only fallback on network errors, not on HTTP errors
            if (firstResult is ApiResult.Success) {
                return firstResult
            }
            // For HTTP errors, don't fallback - the server is reachable
            if (firstResult is ApiResult.Error && firstResult.code != null) {
                return firstResult
            }
        }

        // Try fallback server if available
        if (settings.hasSecondaryServer && secondUrl.isNotBlank()) {
            Log.d(TAG, "Trying fallback server: $secondUrl")
            val secondApi = getApiForUrl(secondUrl)
                ?: return firstResult ?: ApiResult.Error("Fallback server not configured")

            return try {
                apiCall(secondApi)
            } catch (e: Exception) {
                Log.d(TAG, "Fallback server also failed: ${e.message}")
                // Both servers failed, return a combined error message
                when (e) {
                    is javax.net.ssl.SSLHandshakeException ->
                        ApiResult.Error("Both servers failed. SSL certificate error on fallback server.")
                    else -> ApiResult.Error("Both servers unreachable: ${e.message}")
                }
            }
        }

        // No fallback server, return the first server's error
        return firstResult ?: ApiResult.Error("Connection failed")
    }

    suspend fun testConnection(serverUrl: String, acceptInvalidCerts: Boolean = false): ApiResult<Unit> {
        return try {
            val api = apiFactory.create(serverUrl, acceptInvalidCerts)
            val response = api.ping()
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Server returned ${response.code()}", response.code())
            }
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            ApiResult.Error("SSL certificate error. Enable 'Accept invalid certificates' for self-signed certs.")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Connection failed")
        }
    }

    suspend fun getCars(): ApiResult<List<CarData>> {
        return executeWithFallback { api ->
            try {
                val response = api.getCars()
                if (response.isSuccessful) {
                    val cars = response.body()?.data?.cars ?: emptyList()
                    ApiResult.Success(cars)
                } else {
                    ApiResult.Error("Failed to fetch cars: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e // Let executeWithFallback handle it
            }
        }
    }

    suspend fun getCarStatus(carId: Int): ApiResult<CarStatusWithUnits> {
        return executeWithFallback { api ->
            try {
                val response = api.getCarStatus(carId)
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    val status = data?.status
                    val units = data?.units ?: Units()
                    if (status != null) {
                        ApiResult.Success(CarStatusWithUnits(status, units))
                    } else {
                        ApiResult.Error("No status data returned")
                    }
                } else {
                    ApiResult.Error("Failed to fetch status: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getCharges(
        carId: Int,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<List<ChargeData>> {
        return executeWithFallback { api ->
            try {
                val response = api.getCharges(carId, startDate, endDate, page = 1, show = 50000)
                if (response.isSuccessful) {
                    val charges = response.body()?.data?.charges ?: emptyList()
                    ApiResult.Success(charges)
                } else {
                    ApiResult.Error("Failed to fetch charges: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getChargeDetail(carId: Int, chargeId: Int): ApiResult<ChargeDetail> {
        return executeWithFallback { api ->
            try {
                val response = api.getChargeDetail(carId, chargeId)
                if (response.isSuccessful) {
                    val detail = response.body()?.data?.charge
                    if (detail != null) {
                        ApiResult.Success(detail)
                    } else {
                        ApiResult.Error("No charge detail returned")
                    }
                } else {
                    ApiResult.Error("Failed to fetch charge detail: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getDrives(
        carId: Int,
        startDate: String? = null,
        endDate: String? = null
    ): ApiResult<List<DriveData>> {
        return executeWithFallback { api ->
            try {
                val response = api.getDrives(carId, startDate, endDate, page = 1, show = 50000)
                if (response.isSuccessful) {
                    val drives = response.body()?.data?.drives ?: emptyList()
                    ApiResult.Success(drives)
                } else {
                    ApiResult.Error("Failed to fetch drives: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getDriveDetail(carId: Int, driveId: Int): ApiResult<DriveDetail> {
        return executeWithFallback { api ->
            try {
                val response = api.getDriveDetail(carId, driveId)
                if (response.isSuccessful) {
                    val detail = response.body()?.data?.drive
                    if (detail != null) {
                        ApiResult.Success(detail)
                    } else {
                        ApiResult.Error("No drive detail returned")
                    }
                } else {
                    ApiResult.Error("Failed to fetch drive detail: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getBatteryHealth(carId: Int): ApiResult<BatteryHealth> {
        return executeWithFallback { api ->
            try {
                val response = api.getBatteryHealth(carId)
                if (response.isSuccessful) {
                    val health = response.body()?.data?.batteryHealth
                    if (health != null) {
                        ApiResult.Success(health)
                    } else {
                        ApiResult.Error("No battery health data returned")
                    }
                } else {
                    ApiResult.Error("Failed to fetch battery health: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getUpdates(carId: Int): ApiResult<List<UpdateData>> {
        return executeWithFallback { api ->
            try {
                val response = api.getUpdates(carId, page = 1, show = 50000)
                if (response.isSuccessful) {
                    val updates = response.body()?.data?.updates ?: emptyList()
                    ApiResult.Success(updates)
                } else {
                    ApiResult.Error("Failed to fetch updates: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
}
