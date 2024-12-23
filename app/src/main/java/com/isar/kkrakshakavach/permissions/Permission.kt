package com.isar.kkrakshakavach.permissions

enum class Permission(val permissions: List<String>) {
    Camera(listOf(android.Manifest.permission.CAMERA)),
    Audio(listOf(android.Manifest.permission.RECORD_AUDIO)),
    Location(listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)),
    SMS(listOf(android.Manifest.permission.SEND_SMS));

    companion object {
        fun from(permission: String): Permission {
            return entries.find { it.permissions.contains(permission) }
                ?: throw IllegalArgumentException("Unknown permission: $permission")
        }
    }
}
