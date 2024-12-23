package com.isar.kkrakshakavach.permissions


import android.content.Context
import androidx.appcompat.app.AlertDialog

object PermissionDialog {

    val code = 100


    private fun onDismiss(dialog: AlertDialog) {
        dialog.dismiss()
    }


    fun permissionDialog(
        context: Context,
        permissionTextProvider: PermissionTextProvider,
        isPermanentlyDeclined: Boolean,
        onClick: () -> Unit,

    ) {
        AlertDialog.Builder(context).apply {


            setPositiveButton(
                if (isPermanentlyDeclined) {
                    "Settings"
                } else "OK"

            ) { _, _ ->
                if (isPermanentlyDeclined) {
                    onClick()
                } else onClick()
            }
            setNegativeButton(
                if (isPermanentlyDeclined) {
                    "Cancel"
                } else "Cancel"
            ) { _, _ ->
                onDismiss(this.create())
                onClick()

            }
            setTitle("com.isar.kkrakshakavach.permissions.Permission Required")
            setMessage(permissionTextProvider.getDescription(isPermanentlyDeclined))
        }.show()
    }


}

interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class CameraPermissionTextProvider(text: String) : PermissionTextProvider {
    private val text1 = text
    override fun getDescription(isPermanentlyDeclined: Boolean): String {

        return if (isPermanentlyDeclined) {
            "It seems you Permanently declined $text1 permission.You can go to the app settings to grant it."
        } else {
            "This app need access to your camera so that you can" + "record your camera in emergency"
        }
    }

}
class LocationPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you Permanently declined Location permission." + "You can go to the app settings to grant it."
        } else {
            "This app need access to your location so that you can" + "register your attendance"
        }
    }

}
class AudioPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you Permanently declined Audio permission." + "You can go to the app settings to grant it."
        } else {
            "This app need access to your audio so that you can" + "record your audio in emergency"
        }
    }

}