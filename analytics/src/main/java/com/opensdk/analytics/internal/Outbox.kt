package com.opensdk.analytics.internal

import com.opensdk.analytics.model.AnalyticsEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/**
 * Offline-resilient, file-backed event queue. Android analog of the web SDK's
 * localStorage `Outbox`: batches, caps size, expires stale events, and applies
 * exponential jittered retry backoff. Access is synchronized for thread safety.
 */
internal class Outbox(
    private val dir: File,
    private val maxItems: Int = Constants.OUTBOX_MAX_ITEMS,
    private val maxRetries: Int = Constants.MAX_RETRIES,
    private val maxAgeMs: Long = Constants.MAX_EVENT_AGE_MS
) {
    private val file = File(dir, "outbox.json")
    private val lock = Any()

    data class Record(
        val event: AnalyticsEvent,
        var status: String,       // queued | retryable_failed
        var retryCount: Int,
        var nextRetryAt: Long,
        val createdAt: Long,
        var lastError: String? = null
    )

    fun enqueue(event: AnalyticsEvent, now: Long = System.currentTimeMillis()) = synchronized(lock) {
        val records = read().toMutableList()
        records.add(Record(event, "queued", 0, now, now))
        while (records.size > maxItems) records.removeAt(0)
        write(records)
    }

    fun getDueRecords(limit: Int, now: Long = System.currentTimeMillis()): List<Record> =
        synchronized(lock) {
            read().asSequence()
                .filter { it.status == "queued" || (it.status == "retryable_failed" && it.nextRetryAt <= now) }
                .filter { now - it.createdAt <= maxAgeMs }
                .take(limit)
                .toList()
        }

    fun acknowledge(eventIds: Collection<String>) = synchronized(lock) {
        val accepted = eventIds.toHashSet()
        write(read().filterNot { accepted.contains(it.event.eventId) })
    }

    fun markRetryable(eventIds: Collection<String>, lastError: String?, now: Long = System.currentTimeMillis()) =
        synchronized(lock) {
            val ids = eventIds.toHashSet()
            val updated = read().mapNotNull { r ->
                if (!ids.contains(r.event.eventId)) return@mapNotNull r
                val retryCount = r.retryCount + 1
                if (retryCount > maxRetries) return@mapNotNull null
                r.copy(
                    status = "retryable_failed",
                    retryCount = retryCount,
                    nextRetryAt = now + retryDelayMs(retryCount),
                    lastError = lastError
                )
            }
            write(updated)
        }

    fun size(): Int = synchronized(lock) { read().size }

    fun retryDelayMs(retryCount: Int): Long =
        Constants.BASE_RETRY_DELAY_MS * (1L shl retryCount) + Random.nextInt(0, 250)

    // ---- persistence ----------------------------------------------------

    private fun read(): List<Record> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { recordFromJson(arr.getJSONObject(it)) }
        }.getOrElse { emptyList() }
    }

    private fun write(records: List<Record>) {
        runCatching {
            if (!dir.exists()) dir.mkdirs()
            val arr = JSONArray()
            records.forEach { arr.put(recordToJson(it)) }
            file.writeText(arr.toString())
        }
    }

    private fun recordToJson(r: Record): JSONObject = JSONObject()
        .put("event", EventSerializer.toJson(r.event))
        .put("status", r.status)
        .put("retryCount", r.retryCount)
        .put("nextRetryAt", r.nextRetryAt)
        .put("createdAt", r.createdAt)
        .put("lastError", r.lastError ?: JSONObject.NULL)

    private fun recordFromJson(o: JSONObject): Record = Record(
        event = EventSerializer.fromJson(o.getJSONObject("event")),
        status = o.getString("status"),
        retryCount = o.getInt("retryCount"),
        nextRetryAt = o.getLong("nextRetryAt"),
        createdAt = o.getLong("createdAt"),
        lastError = o.optString("lastError", null)
    )
}
