package com.opensdk.analytics.sample

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.opensdk.analytics.OpenAnalytics

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(button("Track custom event") {
            OpenAnalytics.track("button_clicked", mapOf("cta" to "hero"))
            toast("Tracked: button_clicked")
        })

        root.addView(button("Identify user") {
            OpenAnalytics.identify("usr_123", mapOf("tier" to "pro"))
            toast("Identified usr_123")
        })

        root.addView(button("Track conversion") {
            OpenAnalytics.trackConversion("purchase", mapOf("amount" to 9.99))
            toast("Tracked conversion")
        })

        root.addView(button("Flush now") {
            OpenAnalytics.flush()
            toast("Flushed")
        })

        root.addView(button("Force a crash") {
            throw RuntimeException("Deliberate crash from sample app")
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        // If we crashed last run, prompt the user to submit a report.
        OpenAnalytics.processPendingCrashReports()
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
