# Apex Analytics — Android

The ApexHub Android analytics SDK (`io.github.mr-perfect-252:apex-analytics`).
It sends sessions, screen views, custom events and user-submitted crash reports to
**your ApexHub backend** — the endpoint is fixed, so there is nothing to self-host and
nothing to misconfigure. Built by **Sohan Ananthula**.

- **ApexHub-only** — `https://apex-hub-production.vercel.app/api/v1/track` and
  `/api/v1/crash-report` are baked in.
- **Activated by your app's key** — pass your `pk_live_…` ApexHub public key; every event
  and crash report is attributed to your app.
- **Zero third-party runtime dependencies** (Kotlin stdlib + Android framework + bundled `org.json`).

## Install

```kotlin
dependencies {
    implementation("io.github.mr-perfect-252:apex-analytics:1.0.0")
}
```


## Feature parity with `open-sdk-analytics`

| Web SDK feature | open-analytics-android |
|---|---|
| `initAnalytics` / `track` / `identify` / `resetIdentity` / `flush` | `OpenAnalytics.init/track/identify/resetIdentity/flush` |
| Automatic SPA route tracking | Automatic `screen_view` via `ActivityLifecycleCallbacks` |
| Active session duration | `SessionManager` (30-min inactivity) + `time_in_foreground_ms` |
| Web Vitals / performance | `app_cold_start` timing event |
| Error & rejection capture | Uncaught-exception (crash) capture + `trackError()` |
| Offline-resilient outbox (batch 20, retry) | File-backed `Outbox`, batch 20, exponential jittered retry |
| Visitor identity (`visitor_id`, `is_new_visitor`) | `IdentityManager` in SharedPreferences |
| Self-hosted `server/ingest.js` | Same server, extended with `/api/v1/crash-report` |

## Headline feature: user-submitted crash reports

When the app crashes, a global uncaught-exception handler persists the crash (stack trace,
device, session, app version) to disk before the process dies. On the **next launch**, the
SDK shows a dialog where the user can describe what happened and tap **Send report** — the
crash plus their text is POSTed to **your backend** (`crashReportEndpoint`). If they decline,
nothing is sent. Set `promptForCrashReport = false` to submit silently instead.

The dialog is built programmatically (no XML/resources), so it works in any app.

## Quick start

```kotlin
// Application.onCreate()
OpenAnalytics.init(this, AnalyticsConfig(
    apiKey = "pk_live_…",     // your app's ApexHub public key (required)
    appId  = "my-app",        // optional label for your own reference
    debug  = true
))

// Anywhere
OpenAnalytics.track("checkout_started", properties = mapOf("plan" to "pro"))
OpenAnalytics.identify("usr_123", traits = mapOf("tier" to "enterprise"))
```

```kotlin
// In your main Activity so the crash prompt can be shown on a foreground screen:
override fun onResume() {
    super.onResume()
    OpenAnalytics.processPendingCrashReports()
}
```

The endpoints are fixed to ApexHub — there is no `endpoint` parameter to pass. Sessions,
screen views and cold-start timing are captured automatically.

## Configuration (`AnalyticsConfig`)

| Field | Default | Purpose |
|---|---|---|
| `apiKey` | (**required**) | Your app's `pk_live_…` ApexHub key — activates the SDK and attributes events |
| `appId` | `null` | Label attached to events |
| `endpoint` / `crashReportEndpoint` | fixed (ApexHub) | Not configurable — the SDK talks to ApexHub only |
| `headers` | `{}` | Extra request headers |
| `enabled` | `true` | Master on/off switch |
| `debug` | `false` | Verbose logcat |
| `inactivityTimeoutMs` | `1800000` | Session expiry |
| `flushIntervalMs` | `5000` | Background flush cadence |
| `batchSize` | `20` | Events per flush |
| `promptForCrashReport` | `true` | Prompt vs. silent crash submission |
| `disableAutoScreenView` / `disableAutoCrashCapture` / `disableAutoPerformance` | `false` | Opt-outs |

## Backend

Events and crash reports go to your ApexHub backend only. The `server/` directory holds a
zero-dependency reference ingestion server kept for development, but the published SDK
does not use it.

## Modules

- `analytics/` — the SDK library (`com.opensdk.analytics`).
- `sample/` — a runnable demo app with a "Force a crash" button.
- `server/` — the zero-dependency ingestion server.

## License

Android SDK
Copyright (c) 2026 Sohan Ananthula. All rights reserved.

This SDK is distributed under the Mozilla Public License, v. 2.0. 
Any application integrating this SDK must retain the above copyright notice 
and attribution to Sohan Ananthula within its open-source legal credits or 
documentation.inserted Analytics schema and ingestion design credit: `open-sdk-analytics` by Sohan Ananthula.
