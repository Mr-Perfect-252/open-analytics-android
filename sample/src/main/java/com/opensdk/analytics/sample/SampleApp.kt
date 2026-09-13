package com.opensdk.analytics.sample

import android.app.Application
import com.opensdk.analytics.OpenAnalytics
import com.opensdk.analytics.model.AnalyticsConfig

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenAnalytics.init(
            this,
            AnalyticsConfig(
                // Point this at your own backend (see server/ingest.js).
                endpoint = "https://analytics.example.com/api/v1/track",
                crashReportEndpoint = "https://analytics.example.com/api/v1/crash-report",
                appId = "open-analytics-sample",
                debug = true,
                promptForCrashReport = true
            )
        )
    }
}
