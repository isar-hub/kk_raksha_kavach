package com.isar.kkrakshakavach.permissions

import android.os.Build
import androidx.annotation.RequiresApi

enum class Permission(val permissions: List<String>) {
    Camera(listOf(android.Manifest.permission.CAMERA)),
    Audio(listOf(android.Manifest.permission.RECORD_AUDIO)),
    Location(listOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION)),
    STORAGE(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            emptyList()
//            listOf(android.Manifest.permission.READ_MEDIA_VIDEO,android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    ),
    SMS(listOf(android.Manifest.permission.SEND_SMS));



    companion object {
        fun from(permission: String): Permission {
            return entries.find { it.permissions.contains(permission) }
                ?: throw IllegalArgumentException("Unknown permission: $permission")
        }
    }
}
