package com.opensdk.analytics.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Registers Activity lifecycle callbacks to emit automatic `screen_view` events and
 * measure foreground time — the Android analog of the web SDK's SPA route + active-session
 * tracking. Also exposes the current foreground Activity so the crash dialog can be shown.
 */
internal class ActivityTracker(
    private val onScreenView: (screenName: String) -> Unit,
    private val onForeground: () -> Unit,
    private val onBackground: (foregroundMs: Long) -> Unit
) : Application.ActivityLifecycleCallbacks {

    @Volatile
    var currentActivity: Activity? = null
        private set

    private var startedCount = 0
    private var resumedAt = 0L

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        onScreenView(activity.javaClass.simpleName)
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedCount == 0) {
            resumedAt = System.currentTimeMillis()
            onForeground()
        }
        startedCount++
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount--
        if (startedCount <= 0) {
            startedCount = 0
            val fg = if (resumedAt > 0) System.currentTimeMillis() - resumedAt else 0L
            onBackground(fg)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
