package com.opensdk.analytics.internal

import org.json.JSONObject
import java.util.UUID

/**
 * Session lifecycle with inactivity expiry, mirroring open-sdk-analytics `SessionManager`.
 * A session expires after [inactivityTimeoutMs] of no activity.
 */
internal class SessionManager(
    private val prefs: Prefs,
    private val inactivityTimeoutMs: Long
) {
    private val storageKey = "session"

    data class Session(
        val sessionId: String,
        val startedAt: Long,
        var lastActivityAt: Long
    )

    data class EnsureResult(val sessionId: String, val started: Boolean, val reason: String)

    private var session: Session? = readStored()

    fun ensureActiveSession(now: Long = System.currentTimeMillis()): EnsureResult {
        val current = session ?: readStored()
        if (current == null) {
            val s = createSession(now)
            return EnsureResult(s.sessionId, started = true, reason = "first_visit")
        }
        if (now - current.lastActivityAt > inactivityTimeoutMs) {
            val s = createSession(now)
            return EnsureResult(s.sessionId, started = true, reason = "expired")
        }
        current.lastActivityAt = now
        session = current
        persist(current)
        return EnsureResult(current.sessionId, started = false, reason = "continued")
    }

    fun touch(now: Long = System.currentTimeMillis()) {
        val current = session
        if (current == null) {
            ensureActiveSession(now)
            return
        }
        current.lastActivityAt = now
        persist(current)
    }

    fun getCurrentSessionId(): String? = (session ?: readStored())?.sessionId

    private fun createSession(now: Long): Session {
        val s = Session("s_${UUID.randomUUID()}", now, now)
        session = s
        persist(s)
        return s
    }

    private fun readStored(): Session? {
        val raw = prefs.getString(storageKey) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            Session(o.getString("sessionId"), o.getLong("startedAt"), o.getLong("lastActivityAt"))
        }.getOrNull()
    }

    private fun persist(s: Session) {
        val o = JSONObject()
            .put("sessionId", s.sessionId)
            .put("startedAt", s.startedAt)
            .put("lastActivityAt", s.lastActivityAt)
        prefs.putString(storageKey, o.toString())
    }
}
