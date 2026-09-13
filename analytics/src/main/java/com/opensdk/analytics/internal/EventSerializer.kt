package com.opensdk.analytics.internal

import com.opensdk.analytics.model.AnalyticsEvent
import com.opensdk.analytics.model.EventType
import org.json.JSONObject

/**
 * Serializes [AnalyticsEvent] to the same JSON field layout emitted by
 * open-sdk-analytics (`OpenAnalyticsEvent`), so one backend ingests both web and Android.
 */
internal object EventSerializer {

    fun toJson(e: AnalyticsEvent): JSONObject {
        val o = JSONObject()
        o.put("event_id", e.eventId)
        o.put("event_name", e.eventName)
        o.put("event_type", e.eventType.wire)
        o.put("timestamp", e.timestamp)
        o.put("schema_version", e.schemaVersion)
        o.put("sdk_version", e.sdkVersion)
        o.put("sdk_platform", "android")
        o.putOpt("app_id", e.appId)
        o.put("visitor_id", e.visitorId)
        o.put("session_id", e.sessionId)
        o.putOpt("user_id", e.userId)
        e.isNewVisitor?.let { o.put("is_new_visitor", it) }
        o.putOpt("screen_name", e.screenName)
        if (e.context.isNotEmpty()) o.put("context", JsonUtil.mapToJson(e.context))
        if (e.properties.isNotEmpty()) o.put("properties", JsonUtil.mapToJson(e.properties))
        if (e.userTraits.isNotEmpty()) o.put("user_traits", JsonUtil.mapToJson(e.userTraits))
        return o
    }

    fun fromJson(o: JSONObject): AnalyticsEvent = AnalyticsEvent(
        eventId = o.getString("event_id"),
        eventName = o.getString("event_name"),
        eventType = EventType.fromWire(o.optString("event_type", "custom")),
        timestamp = o.getString("timestamp"),
        schemaVersion = o.optString("schema_version", Constants.SCHEMA_VERSION),
        sdkVersion = o.optString("sdk_version", Constants.SDK_VERSION),
        appId = o.optString("app_id", null),
        visitorId = o.getString("visitor_id"),
        sessionId = o.getString("session_id"),
        userId = o.optString("user_id", null),
        isNewVisitor = if (o.has("is_new_visitor")) o.getBoolean("is_new_visitor") else null,
        screenName = o.optString("screen_name", null),
        context = o.optJSONObject("context")?.let { JsonUtil.jsonToMap(it) } ?: emptyMap(),
        properties = o.optJSONObject("properties")?.let { JsonUtil.jsonToMap(it) } ?: emptyMap(),
        userTraits = o.optJSONObject("user_traits")?.let { JsonUtil.jsonToMap(it) } ?: emptyMap()
    )
}
