package com.isar.kkrakshakavach

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val storageOptions: FirebaseOptions = FirebaseOptions.Builder()
            .setApplicationId("1:576503878859:android:f43e322f03e6ce347ef74e") // Another app's mobile sdk app id if needed
            .setApiKey("AIzaSyDS-2I1vnITe1WdOMCCH-lyKOD83cX_ces") // Same API Key if shared
            .setDatabaseUrl("https://imagine-bc615-default-rtdb.firebaseio.com") // Same Database URL if shared
            .setStorageBucket("imagine-bc615.appspot.com") // Same Storage Bucket if shared
            .build();

        FirebaseApp.initializeApp(this, storageOptions, "StorageApp")
        FirebaseApp.initializeApp(this)
    }
}
