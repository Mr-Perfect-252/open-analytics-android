package com.opensdk.analytics.crash

import com.opensdk.analytics.internal.Constants
import com.opensdk.analytics.internal.JsonUtil
import com.opensdk.analytics.model.CrashReport
import org.json.JSONObject

/** Serializes a [CrashReport] to the JSON body POSTed to the crash-report endpoint. */
internal object CrashSerializer {
    fun toJson(r: CrashReport): JSONObject = JSONObject()
        .put("report_type", "crash")
        .put("schema_version", Constants.SCHEMA_VERSION)
        .put("sdk_version", Constants.SDK_VERSION)
        .put("sdk_platform", "android")
        .put("crash_id", r.crashId)
        .put("timestamp", r.isoTimestamp)
        .put("timestamp_ms", r.timestamp)
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
}
