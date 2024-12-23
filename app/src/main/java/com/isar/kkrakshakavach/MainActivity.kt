package com.isar.kkrakshakavach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.isar.kkrakshakavach.databinding.ActivityMainBinding
import com.isar.kkrakshakavach.permissions.CameraPermissionTextProvider
import com.isar.kkrakshakavach.permissions.PermissionDialog
import com.isar.kkrakshakavach.permissions.PermissionDialog.code
import com.isar.kkrakshakavach.permissions.PermissionDialog.permissionDialog

private lateinit var binding: ActivityMainBinding
private val permissionsToRequest = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.SEND_SMS
)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setUpToolbar()
        navGraphImpl()

    }






    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)

//            supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }

    private fun navGraphImpl() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)
    }
}
