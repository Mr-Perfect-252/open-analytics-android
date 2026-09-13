package com.opensdk.analytics.crash

import android.content.Context
import com.opensdk.analytics.internal.DeviceContext
import com.opensdk.analytics.model.AnalyticsConfig
import com.opensdk.analytics.model.CrashReport
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * A [Thread.UncaughtExceptionHandler] that persists a crash to disk before the process dies.
 * The saved report is surfaced on the next launch so the user can add a description and submit
 * it to the configured backend. Chains to any previously installed handler.
 */
internal class CrashHandler(
    private val appContext: Context,
    private val config: AnalyticsConfig,
    private val store: CrashStore,
    private val identity: () -> Triple<String?, String?, String?> // visitorId, sessionId, userId
) : Thread.UncaughtExceptionHandler {

    private val previous: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching { store.save(buildReport(thread, throwable)) }
        // Preserve default behavior (crash dialog / process kill).
        previous?.uncaughtException(thread, throwable)
            ?: run {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
    }

    private fun buildReport(thread: Thread, throwable: Throwable): CrashReport {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val (visitorId, sessionId, userId) = identity()
        val now = System.currentTimeMillis()
        return CrashReport(
            crashId = "c_${UUID.randomUUID()}",
            timestamp = now,
            isoTimestamp = iso(now),
            exceptionClass = throwable.javaClass.name,
            message = throwable.message,
            stackTrace = sw.toString().take(20_000),
            threadName = thread.name,
            userDescription = null,
            visitorId = visitorId,
            sessionId = sessionId,
            userId = userId,
            appId = config.appId,
            appVersionName = DeviceContext.appVersionName(appContext),
            appVersionCode = DeviceContext.appVersionCode(appContext),
            device = DeviceContext.collect(appContext)
        )
    }

    private fun iso(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMs))
    }
}
