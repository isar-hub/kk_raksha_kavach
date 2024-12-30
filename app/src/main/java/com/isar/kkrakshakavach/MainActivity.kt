package com.isar.kkrakshakavach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.*
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.isar.kkrakshakavach.databinding.ActivityMainBinding
import com.isar.kkrakshakavach.permissions.CameraPermissionTextProvider
import com.isar.kkrakshakavach.permissions.PermissionDialog
import com.isar.kkrakshakavach.permissions.PermissionDialog.code
import com.isar.kkrakshakavach.permissions.PermissionDialog.permissionDialog
import com.isar.kkrakshakavach.utils.GpsStatusListener
import com.isar.kkrakshakavach.utils.startGps

private lateinit var binding: ActivityMainBinding
private val permissionsToRequest = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.SEND_SMS
)

class MainActivity : AppCompatActivity() {
    @RequiresApi(VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setUpToolbar()
        navGraphImpl()
        observeGps()



    }



    @RequiresApi(VERSION_CODES.O)
    private fun observeGps() {
        val gpsStatusListener = GpsStatusListener(this)
        gpsStatusListener.observe(this) { isGpson ->
            if (!isGpson) {
                this.startGps(resultLauncher)
                Toast.makeText(this, "Gps is off", Toast.LENGTH_SHORT).show()

            }
        }

    }
    @RequiresApi(Build.VERSION_CODES.O)
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == RESULT_OK) {
            Toast.makeText(this, "Gps is on", Toast.LENGTH_SHORT).show()

        } else if (activityResult.resultCode == RESULT_CANCELED) {

        }
    }
//    private fun requestPermissions() {
//        val permissions = arrayOf(
//            Manifest.permission.SEND_SMS,
//            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION
//        )
//
//        ActivityCompat.requestPermissions(this, permissions, 1)
//    }



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
