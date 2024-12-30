package com.isar.kkrakshakavach.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.location.LocationManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.LiveData
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

class GpsStatusListener(private val context: Context) : LiveData<Boolean>() {

    var isGpsStatusChanged: Boolean? = null

    override fun onActive() {
        super.onActive()
        registerReceiver()
        checkGpsStatus()
    }

    override fun onInactive() {
        super.onInactive()
        unregisterReceiver()
    }

    private val gpsStatusReceiver = object : BroadcastReceiver() {

        override fun onReceive(p0: Context?, p1: Intent?) {
            checkGpsStatus()
        }

    }

    private fun checkGpsStatus() {
        val isGpsOn = isLocationEnable()

        // Post value only if the GPS status changes
        if (isGpsStatusChanged == null || isGpsStatusChanged != isGpsOn) {
            postValue(isGpsOn)
            isGpsStatusChanged = isGpsOn
        }

    }


    private fun isLocationEnable() = context.getSystemService(LocationManager::class.java)
        .isProviderEnabled(LocationManager.GPS_PROVIDER)

    private fun registerReceiver() = context.registerReceiver(
        gpsStatusReceiver,
        IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
    )

    private fun unregisterReceiver() = context.unregisterReceiver(gpsStatusReceiver)

}
fun Context.startGps(resultLauncher: ActivityResultLauncher<IntentSenderRequest>){
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,3000)
        .build()
    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .build()
    val task = LocationServices.getSettingsClient(this).checkLocationSettings(builder)
    task.addOnFailureListener{
        if (it is ResolvableApiException){
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(it.resolution).build()
                resultLauncher.launch(intentSenderRequest)

            }catch (e : IntentSender.SendIntentException){
                CommonMethods.showLogs("GPS","Exception : ${e.message}")
            }
        }

    }
}