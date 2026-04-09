package com.matedroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.matedroid.BuildConfig
import com.matedroid.data.local.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only receiver for switching the Teslamate API endpoint via ADB.
 *
 * Usage:
 *   adb shell am broadcast -a com.matedroid.SET_ENDPOINT --es url "http://host:port"
 *
 * Silently ignored in release builds.
 */
class DebugEndpointReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SettingsEntryPoint {
        fun settingsDataStore(): SettingsDataStore
    }

    companion object {
        private const val TAG = "DebugEndpointReceiver"
        const val ACTION = "com.matedroid.SET_ENDPOINT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        if (intent.action != ACTION) return

        val url = intent.getStringExtra("url")
        if (url.isNullOrBlank()) {
            Log.w(TAG, "Missing or empty 'url' extra")
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsEntryPoint::class.java
        )
        val settingsDataStore = entryPoint.settingsDataStore()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                settingsDataStore.saveServerUrl(url)
                Log.i(TAG, "API endpoint switched to: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch endpoint", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
