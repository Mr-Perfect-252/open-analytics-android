package com.opensdk.analytics.crash

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.opensdk.analytics.model.CrashReport

/**
 * A dependency-free dialog that lets the user describe what happened before a crash
 * report is submitted. Built programmatically so no host-app resources are required.
 */
internal object CrashReportDialog {

    fun show(
        context: Context,
        report: CrashReport,
        title: String,
        messagePrompt: String,
        submitLabel: String,
        dismissLabel: String,
        onSubmit: (description: String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val pad = (16 * context.resources.displayMetrics.density).toInt()

        val explanation = TextView(context).apply {
            val detail = report.message?.takeIf { it.isNotBlank() } ?: report.exceptionClass
            text = "$messagePrompt\n\n($detail)"
            setPadding(0, 0, 0, pad / 2)
        }

        val input = EditText(context).apply {
            hint = "What were you doing when the app crashed? (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.TOP or Gravity.START
            minLines = 3
            maxLines = 6
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(explanation)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setCancelable(true)
            .setPositiveButton(submitLabel) { d, _ ->
                onSubmit(input.text?.toString()?.trim().orEmpty())
                d.dismiss()
            }
            .setNegativeButton(dismissLabel) { d, _ ->
                onDismiss()
                d.dismiss()
            }
            .setOnCancelListener { onDismiss() }
            .show()
    }
}
