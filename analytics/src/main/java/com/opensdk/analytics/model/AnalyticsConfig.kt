package com.opensdk.analytics.model

/**
 * Configuration for the analytics SDK. Mirrors `AnalyticsConfig` from
 * open-sdk-analytics, adapted for Android.
 */
data class AnalyticsConfig(
    /** Target endpoint for tracking ingestion, e.g. "https://analytics.you.com/api/v1/track". */
    val endpoint: String,

    /**
     * Endpoint that receives user-submitted crash reports.
     * Defaults to sibling "/api/v1/crash-report" derived from [endpoint] when null.
     */
    val crashReportEndpoint: String? = null,

    /** Application or project ID. */
    val appId: String? = null,

    /** Optional bearer/API token sent as `Authorization`. */
    val apiKey: String? = null,

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
    val promptForCrashReport: Boolean = true
)
