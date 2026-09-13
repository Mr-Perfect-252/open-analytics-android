package com.opensdk.analytics.crash

import com.opensdk.analytics.internal.JsonUtil
import com.opensdk.analytics.model.CrashReport
import org.json.JSONObject
import java.io.File

/**
 * Persists crash reports to disk so they survive the process death caused by the crash,
 * and can be surfaced (with a user description) on the next launch.
 */
internal class CrashStore(private val dir: File) {

    init {
        if (!dir.exists()) dir.mkdirs()
    }

    fun save(report: CrashReport) {
        runCatching {
            File(dir, "${report.crashId}.json").writeText(toJson(report).toString())
        }
    }

    /** Oldest-first list of pending crash reports. */
    fun pending(): List<CrashReport> {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
        return files.sortedBy { it.lastModified() }
            .mapNotNull { f -> runCatching { fromJson(JSONObject(f.readText())) }.getOrNull() }
    }

    fun delete(crashId: String) {
        runCatching { File(dir, "$crashId.json").delete() }
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun toJson(r: CrashReport): JSONObject = JSONObject()
        .put("crash_id", r.crashId)
        .put("timestamp", r.timestamp)
        .put("iso_timestamp", r.isoTimestamp)
        .put("exception_class", r.exceptionClass)
        .putOpt("message", r.message)
        .put("stack_trace", r.stackTrace)
        .put("thread_name", r.threadName)
        .putOpt("user_description", r.userDescription)
        .putOpt("visitor_id", r.visitorId)
        .putOpt("session_id", r.sessionId)
        .putOpt("user_id", r.userId)
        .putOpt("app_id", r.appId)
        .putOpt("app_version_name", r.appVersionName)
        .putOpt("app_version_code", r.appVersionCode)
        .put("device", JsonUtil.mapToJson(r.device))

    private fun fromJson(o: JSONObject): CrashReport = CrashReport(
        crashId = o.getString("crash_id"),
        timestamp = o.getLong("timestamp"),
        isoTimestamp = o.getString("iso_timestamp"),
        exceptionClass = o.getString("exception_class"),
        message = o.optString("message", null),
        stackTrace = o.getString("stack_trace"),
        threadName = o.optString("thread_name", "unknown"),
        userDescription = o.optString("user_description", null),
        visitorId = o.optString("visitor_id", null),
        sessionId = o.optString("session_id", null),
        userId = o.optString("user_id", null),
        appId = o.optString("app_id", null),
        appVersionName = o.optString("app_version_name", null),
        appVersionCode = if (o.has("app_version_code")) o.getLong("app_version_code") else null,
        device = o.optJSONObject("device")?.let { JsonUtil.jsonToMap(it) } ?: emptyMap()
    )
}
