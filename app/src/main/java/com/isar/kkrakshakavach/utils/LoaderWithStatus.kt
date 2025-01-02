package com.isar.kkrakshakavach.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.isar.kkrakshakavach.R

object LoaderWithStatus {

    private var dialog: Dialog? = null

    fun show(context: Context, message: String) {
        if (dialog == null) {
            dialog = Dialog(context).apply {
                setCancelable(false)
                setContentView(LayoutInflater.from(context).inflate(R.layout.progress_dialog, null))
            }
        }

        dialog?.findViewById<TextView>(R.id.progress_message)?.text = message
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}