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
                // Your app's ApexHub public key (Console → your app → Settings).
                // The SDK always talks to the ApexHub backend; nothing else to configure.
                apiKey = "pk_live_REPLACE_WITH_YOUR_KEY",
                appId = "apex-analytics-sample",
                debug = true,
                promptForCrashReport = true
            )
        )
    }
}
