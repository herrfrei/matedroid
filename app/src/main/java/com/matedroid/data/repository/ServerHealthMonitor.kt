package com.matedroid.data.repository

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.matedroid.data.local.SettingsDataStore
import com.matedroid.di.TeslamateApiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current server availability status.
 */
enum class ServerPreference {
    /** Primary server is available, use it first */
    PRIMARY,
    /** Primary server is unavailable, use secondary first */
    SECONDARY_ONLY,
    /** No servers are available or configured */
    NONE,
    /** Health check hasn't completed yet */
    UNKNOWN
}

/**
 * Monitors the health of configured Teslamate servers and determines which one to use.
 *
 * This monitor runs periodic health checks (ping) against both primary and secondary servers
 * to proactively determine availability. This avoids waiting for connection timeouts when
 * switching between network contexts (e.g., VPN on/off).
 *
 * The monitor only runs when:
 * - The app is in the foreground
 * - A secondary server is configured (otherwise there's no need for health checks)
 */
@Singleton
class ServerHealthMonitor @Inject constructor(
    private val apiFactory: TeslamateApiFactory,
    private val settingsDataStore: SettingsDataStore
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ServerHealthMonitor"
        private const val HEALTH_CHECK_INTERVAL_MS = 10_000L // 10 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var isAppInForeground = false

    private val _serverPreference = MutableStateFlow(ServerPreference.UNKNOWN)
    val serverPreference: StateFlow<ServerPreference> = _serverPreference.asStateFlow()

    init {
        // Register for app lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        startMonitoring()
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        stopMonitoring()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            Log.d(TAG, "Starting health monitoring")

            // Run first check immediately
            checkServersHealth()

            // Then run periodically
            while (isAppInForeground) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                if (isAppInForeground) {
                    checkServersHealth()
                }
            }
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Stopping health monitoring")
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * Triggers an immediate health check.
     * Useful when settings change or network connectivity changes.
     */
    fun checkNow() {
        scope.launch {
            checkServersHealth()
        }
    }

    private suspend fun checkServersHealth() {
        val settings = settingsDataStore.settings.first()

        // If no primary server is configured, nothing to do
        if (settings.serverUrl.isBlank()) {
            _serverPreference.value = ServerPreference.NONE
            return
        }

        // If no secondary server is configured, always use primary (no need for health checks)
        if (!settings.hasSecondaryServer) {
            _serverPreference.value = ServerPreference.PRIMARY
            return
        }

        // Check primary server availability
        val primaryAvailable = pingServer(settings.serverUrl)

        val newPreference = if (primaryAvailable) {
            ServerPreference.PRIMARY
        } else {
            // Primary failed, check if secondary is available
            val secondaryAvailable = pingServer(settings.secondaryServerUrl)
            if (secondaryAvailable) {
                ServerPreference.SECONDARY_ONLY
            } else {
                ServerPreference.NONE
            }
        }

        if (_serverPreference.value != newPreference) {
            Log.d(TAG, "Server preference changed: ${_serverPreference.value} -> $newPreference")
        }
        _serverPreference.value = newPreference
    }

    private suspend fun pingServer(url: String): Boolean {
        return try {
            val api = apiFactory.create(url)
            val response = api.ping()
            response.isSuccessful
        } catch (e: Exception) {
            Log.d(TAG, "Ping failed for $url: ${e.message}")
            false
        }
    }
}
