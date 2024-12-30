package com.isar.kkrakshakavach.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.isar.kkrakshakavach.db.DbClassHelper
import com.isar.kkrakshakavach.utils.CommonMethods

class SOSRepository(
    private val dbHelper: DbClassHelper,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val context: Context
) {
    fun getAllContacts(): List<String> {
        val contactList = ArrayList<String>()
        val cursor = dbHelper.getAllContacts()
        cursor.use {
            while (it.moveToNext()) {
                contactList.add(it.getString(it.getColumnIndexOrThrow(DbClassHelper.COL_PHONE )))
            }
        }
        return contactList
    }

    fun getLocation(context: Context, callback: (LocationUpdate) -> Unit) {
        if (context.isGpsEnabled()) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            callback(LocationUpdate.Done(location))
                        } else {
                            callback(LocationUpdate.LocationError)
                        }
                    }
                    .addOnFailureListener {
                        callback(LocationUpdate.LocationError)
                    }
            } else {
                callback(LocationUpdate.PermissionDenied)
            }
        } else {
            callback(LocationUpdate.GpsNotEnabled)
        }
    }



    private fun Context.isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun sendSMS(contact: String, message: String,context: Context,viewModel: SOSViewmodel) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(contact, null, message, null, null)
            CommonMethods.showLogs("SMS","SMS SEND TO $contact, and $message")
            viewModel.isSendingSos.postValue(false)

        } catch (e: Exception) {
            CommonMethods.showLogs("SMS","FAILED : ${e.message}")
            Toast.makeText(context, "Failed to send SMS to ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}