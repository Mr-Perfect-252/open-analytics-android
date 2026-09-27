package com.opensdk.analytics

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.opensdk.analytics.crash.CrashHandler
import com.opensdk.analytics.crash.CrashReportDialog
import com.opensdk.analytics.crash.CrashSerializer
import com.opensdk.analytics.crash.CrashStore
import com.opensdk.analytics.internal.ActivityTracker
import com.opensdk.analytics.internal.Constants
import com.opensdk.analytics.internal.DeviceContext
import com.opensdk.analytics.internal.EventSerializer
import com.opensdk.analytics.internal.HttpTransport
import com.opensdk.analytics.internal.IdentityManager
import com.opensdk.analytics.internal.Log
import com.opensdk.analytics.internal.Outbox
import com.opensdk.analytics.internal.Prefs
import com.opensdk.analytics.internal.SessionManager
import com.opensdk.analytics.model.AnalyticsConfig
import com.opensdk.analytics.model.AnalyticsEvent
import com.opensdk.analytics.model.CrashReport
import com.opensdk.analytics.model.EventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Public entry point for the Apex analytics SDK. Talks to the **ApexHub backend only**.
 * API: init / track / trackScreen / trackError / identify / resetIdentity / flush, plus
 * user-submitted crash reporting.
 *
 * ```
 * OpenAnalytics.init(this, AnalyticsConfig(
 *     apiKey = "pk_live_…",     // your app's ApexHub public key
 *     appId = "my-app", debug = true
 * ))
 * OpenAnalytics.track("checkout_started", properties = mapOf("plan" to "pro"))
 * OpenAnalytics.identify("usr_123", traits = mapOf("tier" to "enterprise"))
 * ```
 */
@SuppressLint("StaticFieldLeak") // holds application context only
object OpenAnalytics {

    private lateinit var appContext: Context
    private lateinit var config: AnalyticsConfig
    private lateinit var prefs: Prefs
    private lateinit var identity: IdentityManager
    private lateinit var session: SessionManager
    private lateinit var outbox: Outbox
    private lateinit var transport: HttpTransport
    private lateinit var crashStore: CrashStore
    private var activityTracker: ActivityTracker? = null

    private val worker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "open-analytics-worker").apply { isDaemon = true }
    }

    @Volatile private var initialized = false
    @Volatile private var lastFlushAt: Long = 0L
    @Volatile private var lastError: String? = null
    private val processStartMs = System.currentTimeMillis()

    // ---- lifecycle ------------------------------------------------------

    /** Initialize the SDK. Safe to call once, typically from Application.onCreate(). */
    @JvmStatic
    fun init(context: Context, config: AnalyticsConfig) {
        if (initialized) return
        this.appContext = context.applicationContext
        this.config = config
        this.prefs = Prefs(appContext, config.storagePrefix)
        this.identity = IdentityManager(prefs)
        this.session = SessionManager(prefs, config.inactivityTimeoutMs)
        this.outbox = Outbox(java.io.File(appContext.filesDir, "open-analytics"))
        this.transport = HttpTransport(config)
        this.crashStore = CrashStore(java.io.File(appContext.filesDir, "open-analytics-crashes"))
        initialized = true

        if (!config.disableAutoCrashCapture) installCrashHandler()
        registerActivityCallbacks()
        scheduleFlush()

        // Establish visitor + session and (optionally) emit a session_start.
        val visitor = identity.getOrCreateVisitorIdentity()
        val ensure = session.ensureActiveSession()
        if (ensure.started) {
            enqueue(buildEvent("session_start", EventType.TRAFFIC, emptyMap(), emptyMap()))
        }
        if (!config.disableAutoPerformance) {
            val coldStart = System.currentTimeMillis() - processStartMs
            enqueue(
                buildEvent(
                    "app_cold_start", EventType.TRAFFIC, emptyMap(),
                    mapOf("cold_start_ms" to coldStart)
                )
            )
        }
        Log.d(config, "Initialized. visitor=${visitor.visitorId} session=${ensure.sessionId}")
    }

    /** True once [init] has been called. */
    @JvmStatic
    fun isInitialized(): Boolean = initialized

    // ---- public tracking API -------------------------------------------

    /** Track a custom event with optional properties/context. */
    @JvmStatic
    @JvmOverloads
    fun track(
        eventName: String,
        properties: Map<String, Any?> = emptyMap(),
        context: Map<String, Any?> = emptyMap(),
        eventType: EventType = EventType.CUSTOM
    ) {
        if (!ready()) return
        session.touch()
        enqueue(buildEvent(eventName, eventType, context, properties))
    }

    /** Track a conversion event. */
    @JvmStatic
    @JvmOverloads
    fun trackConversion(eventName: String, properties: Map<String, Any?> = emptyMap()) =
        track(eventName, properties, eventType = EventType.CONVERSION)

    /** Manually track a screen view. */
    @JvmStatic
    fun trackScreen(screenName: String) {
        if (!ready()) return
        session.touch()
        enqueue(buildEvent("screen_view", EventType.TRAFFIC, emptyMap(), emptyMap(), screenName))
    }

    /** Track a non-fatal error/exception. */
    @JvmStatic
    @JvmOverloads
    fun trackError(throwable: Throwable, properties: Map<String, Any?> = emptyMap()) {
        if (!ready()) return
        val props = HashMap(properties)
        props["exception_class"] = throwable.javaClass.name
        props["message"] = throwable.message
        props["stack"] = android.util.Log.getStackTraceString(throwable).take(4000)
        enqueue(buildEvent("error", EventType.ERROR, emptyMap(), props))
    }

    /** Associate the current visitor with a known user id and traits. */
    @JvmStatic
    @JvmOverloads
    fun identify(userId: String, traits: Map<String, Any?> = emptyMap()) {
        if (!ready()) return
        identity.identify(userId, traits)
        enqueue(buildEvent("identify", EventType.IDENTIFY, emptyMap(), emptyMap()))
    }

    /** Clear the current user identity (keeps the anonymous visitor id). */
    @JvmStatic
    fun resetIdentity() {
        if (!ready()) return
        identity.reset()
    }

    /** Force an immediate flush of queued events. */
    @JvmStatic
    fun flush() {
        if (!ready()) return
        worker.execute { flushOnce() }
    }

    /** Snapshot of internal health, mirroring the web SDK's getAnalyticsHealth(). */
    @JvmStatic
    fun health(): Map<String, Any?> = if (!initialized) mapOf("initialized" to false) else mapOf(
        "initialized" to true,
        "enabled" to config.enabled,
        "visitor_id" to identity.getOrCreateVisitorIdentity().visitorId,
        "session_id" to session.getCurrentSessionId(),
        "user_id" to identity.getUserId(),
        "outbox_size" to outbox.size(),
        "endpoint" to config.endpoint,
        "last_flush_at" to lastFlushAt,
        "last_error" to lastError
    )

    // ---- crash reporting -----------------------------------------------

    /**
     * If a crash was captured on a previous run, surface it now. With
     * [AnalyticsConfig.promptForCrashReport] true, shows a dialog on the current
     * foreground Activity so the user can describe what happened before submitting;
     * otherwise submits silently. Call this from your main Activity's onResume/onCreate.
     */
    @JvmStatic
    fun processPendingCrashReports() {
        if (!ready()) return
        val pending = crashStore.pending()
        if (pending.isEmpty()) return

        if (!config.promptForCrashReport) {
            worker.execute { pending.forEach { submitCrash(it) } }
            return
        }

        val activity = activityTracker?.currentActivity
        if (activity == null || activity.isFinishing) {
            Log.d(config, "No foreground activity to prompt; will retry on next launch.")
            return
        }
        val report = pending.first()
        activity.runOnUiThread {
            CrashReportDialog.show(
                context = activity,
                report = report,
                title = "Report a problem",
                messagePrompt = "The app closed unexpectedly last time. Sending a report helps us fix it.",
                submitLabel = "Send report",
                dismissLabel = "Don't send",
                onSubmit = { description ->
                    worker.execute { submitCrash(report.withUserDescription(description)) }
                },
                onDismiss = {
                    worker.execute { crashStore.delete(report.crashId) }
                }
            )
        }
    }

    private fun submitCrash(report: CrashReport) {
        val url = config.crashReportEndpoint
        val code = transport.postRaw(url, CrashSerializer.toJson(report).toString())
        if (code in 200..299) {
            crashStore.delete(report.crashId)
            Log.d(config, "Crash ${report.crashId} submitted.")
        } else {
            lastError = "crash submit http $code"
            Log.d(config, "Crash submit failed ($code); keeping for retry.")
        }
    }

    // ---- internals ------------------------------------------------------

    private fun ready(): Boolean = initialized && config.enabled

    private fun installCrashHandler() {
        CrashHandler(
            appContext = appContext,
            config = config,
            store = crashStore,
            identity = {
                Triple(
                    identity.getOrCreateVisitorIdentity().visitorId,
                    session.getCurrentSessionId(),
                    identity.getUserId()
                )
            }
        ).install()
    }

    private fun registerActivityCallbacks() {
        val app = appContext as? Application ?: return
        val tracker = ActivityTracker(
            onScreenView = { screen ->
                if (!config.disableAutoScreenView && ready()) {
                    session.touch()
                    enqueue(buildEvent("screen_view", EventType.TRAFFIC, emptyMap(), emptyMap(), screen))
                }
            },
            onForeground = {
                if (ready()) session.ensureActiveSession()
            },
            onBackground = { foregroundMs ->
                if (ready()) {
                    enqueue(
                        buildEvent(
                            "app_background", EventType.TRAFFIC, emptyMap(),
                            mapOf("time_in_foreground_ms" to foregroundMs)
                        )
                    )
                    worker.execute { flushOnce() }
                }
            }
        )
        this.activityTracker = tracker
        app.registerActivityLifecycleCallbacks(tracker)
    }

    private fun scheduleFlush() {
        worker.scheduleWithFixedDelay(
            { runCatching { flushOnce() }.onFailure { lastError = it.message } },
            config.flushIntervalMs, config.flushIntervalMs, TimeUnit.MILLISECONDS
        )
    }

    private fun flushOnce() {
        if (!ready()) return
        val due = outbox.getDueRecords(config.batchSize)
        if (due.isEmpty()) return
        val result = transport.send(due.map { it.event })
        if (result.accepted.isNotEmpty()) outbox.acknowledge(result.accepted)
        if (result.retryable.isNotEmpty()) outbox.markRetryable(result.retryable, lastError)
        lastFlushAt = System.currentTimeMillis()
    }

    private fun enqueue(event: AnalyticsEvent) {
        outbox.enqueue(event)
    }

    private fun buildEvent(
        name: String,
        type: EventType,
        context: Map<String, Any?>,
        properties: Map<String, Any?>,
        screenName: String? = null
    ): AnalyticsEvent {
        val visitor = identity.getOrCreateVisitorIdentity()
        val ctx = HashMap<String, Any?>()
        ctx["device"] = DeviceContext.collect(appContext)
        ctx.putAll(context)
        return AnalyticsEvent(
            eventId = "e_${UUID.randomUUID()}",
            eventName = name,
            eventType = type,
            timestamp = iso(System.currentTimeMillis()),
            schemaVersion = Constants.SCHEMA_VERSION,
            sdkVersion = Constants.SDK_VERSION,
            appId = config.appId,
            visitorId = visitor.visitorId,
            sessionId = session.getCurrentSessionId() ?: session.ensureActiveSession().sessionId,
            userId = identity.getUserId(),
            isNewVisitor = visitor.isNewVisitor,
            screenName = screenName,
            context = ctx,
            properties = properties,
            userTraits = identity.getUserTraits()
        )
    }

    private fun iso(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMs))
    }
}
