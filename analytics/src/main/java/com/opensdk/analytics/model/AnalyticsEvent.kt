package com.opensdk.analytics.model

/** Event category, mirroring open-sdk-analytics `EventType`. */
enum class EventType(val wire: String) {
    TRAFFIC("traffic"),
    CUSTOM("custom"),
    CONVERSION("conversion"),
    IDENTIFY("identify"),
    ERROR("error"),
    CRASH("crash");

    companion object {
        fun fromWire(value: String): EventType =
            values().firstOrNull { it.wire == value } ?: CUSTOM
    }
}

/**
 * A single analytics event. Field names match the JSON wire format emitted by
 * open-sdk-analytics (`OpenAnalyticsEvent`) so the same ingestion backend accepts both.
 */
data class AnalyticsEvent(
    val eventId: String,
    val eventName: String,
    val eventType: EventType,
    val timestamp: String,
    val schemaVersion: String,
    val sdkVersion: String,
    val appId: String?,
    val visitorId: String,
    val sessionId: String,
    val userId: String?,
    val isNewVisitor: Boolean?,
    val screenName: String?,
    val context: Map<String, Any?> = emptyMap(),
    val properties: Map<String, Any?> = emptyMap(),
    val userTraits: Map<String, Any?> = emptyMap()
)
