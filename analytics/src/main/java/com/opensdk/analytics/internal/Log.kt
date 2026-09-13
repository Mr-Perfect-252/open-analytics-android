package com.opensdk.analytics.internal

import com.opensdk.analytics.model.AnalyticsConfig

/** Debug logging gated by [AnalyticsConfig.debug]. */
internal object Log {
    fun d(config: AnalyticsConfig, message: String) {
        if (config.debug) android.util.Log.d(Constants.LOG_TAG, message)
    }

    fun w(message: String, t: Throwable? = null) {
        android.util.Log.w(Constants.LOG_TAG, message, t)
    }
}
