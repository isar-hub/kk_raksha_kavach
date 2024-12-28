package com.isar.kkrakshakavach.utils

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.isar.kkrakshakavach.R


object CommonMethods {

    fun showLogs(tag : String,msg : String){
        Log.e(tag,msg)
    }

    fun showSnackBar(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG, isSuccess : Boolean = true) {

        Snackbar.make(view, message, duration).apply { setBackgroundTint(
             view.resources.getColor( if(isSuccess) R.color.green else R.color.red)
        ) }.show()
    }

    private var loaderDialog: Dialog? = null

    fun showLoader(context: Context) {
        if (loaderDialog?.isShowing == true) return // Prevent multiple loaders

        loaderDialog = Dialog(context).apply {
            val progressBar = ProgressBar(context) // Create a simple ProgressBar
            setContentView(progressBar)

            setCancelable(false) // Prevent dismissing loader via back button
            window?.setBackgroundDrawableResource(android.R.color.transparent) // Transparent background
            show()
        }
    }


    fun initializeFirebase(context : Context){


    }
    // Function to hide the loader
    fun hideLoader() {
        loaderDialog?.dismiss()
        loaderDialog = null
    }
}
