package com.opensdk.analytics.internal

import com.opensdk.analytics.model.AnalyticsConfig
import com.opensdk.analytics.model.AnalyticsEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends event batches to the configured endpoint over HttpURLConnection (zero deps).
 * Returns the set of accepted event ids so the Outbox can drop them.
 */
internal class HttpTransport(private val config: AnalyticsConfig) {

    data class Result(val accepted: List<String>, val retryable: List<String>)

    /** POST a batch; on any failure the whole batch is treated as retryable. */
    fun send(events: List<AnalyticsEvent>): Result {
        if (events.isEmpty()) return Result(emptyList(), emptyList())
        val ids = events.map { it.eventId }
        val payload = JSONObject().put("events", JSONArray().apply {
            events.forEach { put(EventSerializer.toJson(it)) }
        })
        return try {
            val code = postJson(config.endpoint, payload.toString(), idempotencyKey(ids))
            if (code in 200..299) Result(ids, emptyList())
            else if (code in 400..499 && code != 408 && code != 429) {
                // Non-retryable client error: drop to avoid poison-pill loops.
                Log.d(config, "Dropping batch on client error $code")
                Result(ids, emptyList())
            } else Result(emptyList(), ids)
        } catch (t: Throwable) {
            Log.d(config, "Batch send failed: ${t.message}")
            Result(emptyList(), ids)
        }
    }

    /** POST a raw JSON body (used for crash reports). Returns HTTP status or -1. */
    fun postRaw(url: String, body: String): Int = try {
        postJson(url, body, null)
    } catch (t: Throwable) {
        Log.d(config, "Raw POST failed: ${t.message}")
        -1
    }

    private fun postJson(urlStr: String, body: String, idempotencyKey: String?): Int {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            config.apiKey?.let { setRequestProperty("Authorization", "Bearer $it") }
            idempotencyKey?.let { setRequestProperty("X-Idempotency-Key", it) }
            config.headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            // Drain streams so the connection can be pooled/reused.
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText)
            return code
        } finally {
            conn.disconnect()
        }
    }

    /** FNV-1a based idempotency key, matching the web SDK's buildIdempotencyKey shape. */
    private fun idempotencyKey(ids: List<String>): String {
        if (ids.isEmpty()) return ""
        var hash = 2166136261L
        for (id in ids) {
            for (c in id) {
                hash = hash xor c.code.toLong()
                hash = (hash * 16777619L) and 0xFFFFFFFFL
            }
            hash = hash xor 44L
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        val hex = (hash and 0xFFFFFFFFL).toString(16).padStart(8, '0')
        return "idem_${ids.size}_${ids[0]}_$hex"
    }
}
