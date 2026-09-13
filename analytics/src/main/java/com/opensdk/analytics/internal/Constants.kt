package com.opensdk.analytics.internal

/** Library-wide constants, mirroring open-sdk-analytics defaults. */
internal object Constants {
    const val SDK_VERSION = "1.0.0"
    const val SCHEMA_VERSION = "1.0.0"

    const val INACTIVITY_TIMEOUT_MS = 30L * 60L * 1000L        // 30 minutes
    const val OUTBOX_MAX_ITEMS = 500
    const val BATCH_SIZE = 20
    const val FLUSH_INTERVAL_MS = 5000L
    const val MAX_RETRIES = 7
    const val MAX_EVENT_AGE_MS = 72L * 60L * 60L * 1000L       // 72 hours
    const val BASE_RETRY_DELAY_MS = 2000L
    const val STORAGE_PREFIX = "open.analytics"

    val SENSITIVE_KEY_SUBSTRINGS = listOf(
        "password", "token", "secret", "cookie",
        "authorization", "auth", "apikey", "api_key"
    )

    const val LOG_TAG = "OpenAnalytics"
}
