package com.isar.kkrakshakavach.sos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
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

    fun sendSMS(contact: String, message: String, context: Context, viewModel: SOSViewmodel) {
        try {
            CommonMethods.showLogs("SMS", "Attempting to send SMS...")

            // Step 1: Log contact and message details
            CommonMethods.showLogs("SMS", "Contact: $contact, Message: $message")

            // Step 2: Fetch subscription ID and log it
            val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
            CommonMethods.showLogs("SMS", "Default SMS Subscription ID: $subscriptionId")

            val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)

            // Step 3: Divide message and log parts
            val parts: ArrayList<String> = smsManager.divideMessage(message)
            CommonMethods.showLogs("SMS", "Message divided into ${parts.size} parts: $parts")

            // Step 4: Attempt to send SMS
            smsManager.sendMultipartTextMessage(contact, null, parts, null, null)
            CommonMethods.showLogs("SMS", "SMS sent successfully to $contact")

            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Location Sent", Toast.LENGTH_SHORT).show()
            }

            // Step 5: Update ViewModel and log state
            viewModel.isSendingSos.postValue(Pair(false, ""))
            CommonMethods.showLogs("SMS", "ViewModel updated: isSendingSos set to false")

        } catch (e: Exception) {
            // Step 6: Catch and log the exception
            CommonMethods.showLogs("SMS", "FAILED to send SMS: ${e.message}")

            viewModel.isSendingSos.postValue(Pair(false, ""))
            CommonMethods.showLogs("SMS", "ViewModel updated: isSendingSos set to false due to failure")

            // Step 7: Notify user of the failure
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}