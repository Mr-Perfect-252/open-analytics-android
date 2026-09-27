package com.opensdk.analytics.model

/**
 * Configuration for the Apex analytics SDK.
 *
 * This SDK talks to the **ApexHub backend only** — the ingestion endpoint is fixed
 * (see [APEX_ENDPOINT]); there is no way to point it elsewhere. [apiKey] is your
 * app's ApexHub public key (`pk_live_…`); it activates the SDK and attributes every
 * event to your app. Events are ingested at `POST /api/v1/track` and crash reports at
 * `POST /api/v1/crash-report`, both authenticated with that key.
 */
data class AnalyticsConfig(
    /** Your app's ApexHub public key, e.g. `pk_live_…`. Sent as `Authorization: Bearer`. */
    val apiKey: String,

    /** Optional label for your own reference; attached to every event as `app_id`. */
    val appId: String? = null,

    /** Extra headers to attach to every ingestion request. */
    val headers: Map<String, String> = emptyMap(),

    /** Master on/off switch. */
    val enabled: Boolean = true,

    /** Verbose logcat logging. */
    val debug: Boolean = false,

    /** Session inactivity timeout (ms). Default 30 min. */
    val inactivityTimeoutMs: Long = com.opensdk.analytics.internal.Constants.INACTIVITY_TIMEOUT_MS,

    /** Flush interval (ms). Default 5000. */
    val flushIntervalMs: Long = com.opensdk.analytics.internal.Constants.FLUSH_INTERVAL_MS,

    /** Batch size per flush. Default 20. */
    val batchSize: Int = com.opensdk.analytics.internal.Constants.BATCH_SIZE,

    /** Storage/prefs key prefix. Default "open.analytics". */
    val storagePrefix: String = com.opensdk.analytics.internal.Constants.STORAGE_PREFIX,

    /** Disable automatic screen_view tracking via Activity lifecycle. */
    val disableAutoScreenView: Boolean = false,

    /** Disable automatic uncaught-exception (crash) capture. */
    val disableAutoCrashCapture: Boolean = false,

    /** Disable automatic cold-start performance event. */
    val disableAutoPerformance: Boolean = false,

    /**
     * When true (default), on the next launch after a crash the SDK shows a
     * dialog letting the user describe what happened before submitting.
     * When false, the crash is submitted silently on next launch.
     */
    val promptForCrashReport: Boolean = true,
) {
    /** Event ingestion endpoint — always the ApexHub backend. */
    val endpoint: String get() = APEX_ENDPOINT

    /** Crash-report endpoint — always the ApexHub backend. */
    val crashReportEndpoint: String get() = APEX_CRASH_ENDPOINT

    init {
        require(apiKey.startsWith("pk_live_") || apiKey.startsWith("pk_test_")) {
            "[ApexAnalytics] apiKey must start with 'pk_live_' or 'pk_test_'. Got: $apiKey"
        }
    }

    companion object {
        /** ApexHub event-ingestion endpoint. */
        const val APEX_ENDPOINT = "https://apex-hub-production.vercel.app/api/v1/track"

        /** ApexHub crash-report endpoint. */
        const val APEX_CRASH_ENDPOINT = "https://apex-hub-production.vercel.app/api/v1/crash-report"
    }
}
