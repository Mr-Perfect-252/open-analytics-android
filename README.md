# open-analytics-android

An Android SDK for privacy-friendly, vendor-neutral product analytics — the Android
equivalent of [`open-sdk-analytics`](https://www.npmjs.com/package/open-sdk-analytics)
by **Sohan Ananthula**. Same event schema and self-hostable ingestion backend, plus a
headline Android-only feature: **user-submitted crash reports**.

- **Zero third-party runtime dependencies** (Kotlin stdlib + Android framework + bundled `org.json`).
- **Same wire format** as `open-sdk-analytics`, so one backend ingests both web and Android.
- **Self-hostable** ingestion server included (`server/ingest.js`).

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
    endpoint = "https://analytics.yourdomain.com/api/v1/track",
    crashReportEndpoint = "https://analytics.yourdomain.com/api/v1/crash-report",
    appId = "my-app",
    debug = true,
    promptForCrashReport = true
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

If `crashReportEndpoint` is omitted, it is derived from `endpoint` by replacing the
trailing `/track` with `/crash-report`.

## Configuration (`AnalyticsConfig`)

| Field | Default | Purpose |
|---|---|---|
| `endpoint` | (required) | Event ingestion URL |
| `crashReportEndpoint` | derived from `endpoint` | Crash-report URL |
| `appId` | `null` | Project/app id attached to events |
| `apiKey` | `null` | Sent as `Authorization: Bearer …` |
| `headers` | `{}` | Extra request headers |
| `enabled` | `true` | Master on/off switch |
| `debug` | `false` | Verbose logcat |
| `inactivityTimeoutMs` | `1800000` | Session expiry |
| `flushIntervalMs` | `5000` | Background flush cadence |
| `batchSize` | `20` | Events per flush |
| `promptForCrashReport` | `true` | Prompt vs. silent crash submission |
| `disableAutoScreenView` / `disableAutoCrashCapture` / `disableAutoPerformance` | `false` | Opt-outs |

## Self-hosting the backend

```bash
cd server
node ingest.js       # POST /api/v1/track and /api/v1/crash-report
```

Events append to `events.jsonl`; crash reports append to `crash-reports.jsonl`.

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
