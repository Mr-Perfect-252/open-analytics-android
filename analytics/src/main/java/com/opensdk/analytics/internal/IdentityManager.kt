package com.opensdk.analytics.internal

import org.json.JSONObject
import java.util.UUID

/**
 * Persistent visitor + user identity, mirroring open-sdk-analytics `IdentityManager`.
 * Backed by SharedPreferences (Android has no cookies/localStorage split).
 */
internal class IdentityManager(private val prefs: Prefs) {

    private val visitorKey get() = "visitor_id"
    private val firstSeenKey get() = "first_seen_at"
    private val userKey get() = "user_id"
    private val traitsKey get() = "user_traits"

    private var currentUserId: String? = null
    private var currentTraits: MutableMap<String, Any?> = linkedMapOf()

    init {
        restoreUserIdentity()
    }

    data class VisitorIdentity(
        val visitorId: String,
        val firstSeenAt: Long,
        val isNewVisitor: Boolean
    )

    fun getOrCreateVisitorIdentity(now: Long = System.currentTimeMillis()): VisitorIdentity {
        val existingId = prefs.getString(visitorKey)?.takeIf { it.isNotBlank() }
        val existingFirstSeen = prefs.getString(firstSeenKey)?.toLongOrNull() ?: 0L
        if (existingId != null && existingFirstSeen > 0L) {
            return VisitorIdentity(existingId, existingFirstSeen, isNewVisitor = false)
        }
        val visitorId = existingId ?: "v_${UUID.randomUUID()}"
        prefs.putString(visitorKey, visitorId)
        prefs.putString(firstSeenKey, now.toString())
        return VisitorIdentity(visitorId, now, isNewVisitor = true)
    }

    fun identify(userId: String, traits: Map<String, Any?>?) {
        currentUserId = userId
        if (traits != null) currentTraits.putAll(traits)
        prefs.putString(userKey, userId)
        prefs.putString(traitsKey, JsonUtil.mapToJson(currentTraits).toString())
    }

    fun getUserId(): String? = currentUserId
    fun getUserTraits(): Map<String, Any?> = currentTraits

    fun reset() {
        currentUserId = null
        currentTraits = linkedMapOf()
        prefs.remove(userKey)
        prefs.remove(traitsKey)
    }

    private fun restoreUserIdentity() {
        prefs.getString(userKey)?.let { currentUserId = it }
        prefs.getString(traitsKey)?.let { raw ->
            runCatching { currentTraits = JsonUtil.jsonToMap(JSONObject(raw)).toMutableMap() }
        }
    }
}
