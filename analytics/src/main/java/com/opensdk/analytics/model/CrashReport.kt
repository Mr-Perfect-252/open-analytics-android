package com.opensdk.analytics.model

/**
 * A captured crash awaiting (or ready for) submission. Persisted to disk when an
 * uncaught exception occurs, then surfaced on the next app launch so the user can
 * add a description before it is sent to the backend.
 */
data class CrashReport(
    val crashId: String,
    /** Epoch millis when the crash occurred. */
    val timestamp: Long,
    /** ISO-8601 timestamp string. */
    val isoTimestamp: String,
    val exceptionClass: String,
    val message: String?,
    val stackTrace: String,
    val threadName: String,
    /** Set later from the user's dialog input. */
    val userDescription: String? = null,
    val visitorId: String?,
    val sessionId: String?,
    val userId: String?,
    val appId: String?,
    val appVersionName: String?,
    val appVersionCode: Long?,
    val device: Map<String, Any?> = emptyMap()
) {
    fun withUserDescription(text: String?): CrashReport = copy(userDescription = text)
}
