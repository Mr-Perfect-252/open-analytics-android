package com.opensdk.analytics.internal

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/** Collects a stable device/context map, the Android analog of the web SDK's DeviceContext. */
internal object DeviceContext {

    fun collect(context: Context): Map<String, Any?> {
        val cfg = context.resources.configuration
        val isTablet = (cfg.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >=
            Configuration.SCREENLAYOUT_SIZE_LARGE
        val metrics = context.resources.displayMetrics
        return linkedMapOf(
            "device_type" to if (isTablet) "tablet" else "mobile",
            "platform" to "android",
            "os" to "Android ${Build.VERSION.RELEASE}",
            "os_api" to Build.VERSION.SDK_INT,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "brand" to Build.BRAND,
            "language" to Locale.getDefault().toLanguageTag(),
            "screen_resolution" to "${metrics.widthPixels}x${metrics.heightPixels}",
            "screen_density" to metrics.density
        )
    }

    fun appVersionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (t: Throwable) {
        null
    }

    @Suppress("DEPRECATION")
    fun appVersionCode(context: Context): Long? = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode
        else pi.versionCode.toLong()
    } catch (t: Throwable) {
        null
    }
}
