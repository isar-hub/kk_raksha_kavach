package com.isar.kkrakshakavach.sos

import android.location.Location
sealed class LocationUpdate {
    data class Done(val location: Location) : LocationUpdate()
    object GpsNotEnabled : LocationUpdate()
    object PermissionDenied : LocationUpdate()
    object LocationError : LocationUpdate()
}