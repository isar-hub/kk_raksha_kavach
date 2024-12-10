package com.isar.kkrakshakavach

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.isar.kkrakshakavach.db.DbClassHelper

class SoSFragment : Fragment(), LocationListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

    private val handler = Handler()
    private lateinit var myDB: DbClassHelper
    private var isSendingSOS = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_so_s, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check and request SMS permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 1
            )
        }

        // Initialize location manager and database
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        myDB = DbClassHelper(requireContext())

        // Set up the SOS button click listener
        view.findViewById<MaterialButton>(R.id.bt_sendLocation).setOnClickListener {
            if (!isSendingSOS) {
                isSendingSOS = true
                startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "SOS is already in progress.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun startLocationUpdates() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            Toast.makeText(
                requireContext(),
                "Please enable GPS for location tracking.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Start location updates
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                sendLocation(it)
            } ?: Log.e("LocationError", "Last known location is null, requesting new location.")
            requestNewLocationData()
        }

        // Send location every 5 minutes
        handler.postDelayed(object : Runnable {
            override fun run() {
                requestNewLocationData()
                handler.postDelayed(this, 60000) // 5 minutes
            }
        }, 60000)
    }

    private fun requestNewLocationData() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.e("LocationError", "new location. $location")
                sendLocation(it)
            } ?: Log.e("LocationError", "Unable to retrieve new location.")
        }
    }

    private fun sendLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val message = " I need help! My location is: https://maps.google.com/?q=$lat,$lon"

        val contactList = ArrayList<String>()

        // Retrieve all contacts from the database
        val data: Cursor = myDB.getAllContacts()
        try {
            if (data.count > 0) {
                val phoneIndex = data.getColumnIndex(DbClassHelper.COL_PHONE)
                if (phoneIndex >= 0) {
                    while (data.moveToNext()) {
                        val phone = data.getString(phoneIndex)
                        contactList.add(phone)
                    }
                } else {
                    Log.e("DatabaseError", "Phone column not found in cursor.")
                }
            } else {
                Log.e("DatabaseError", "No contacts found in the database.")
            }
        } catch (e: Exception) {
            Log.e("DatabaseError", "Error retrieving contacts: ${e.message}")
        } finally {
            data.close()
        }

        if (contactList.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No contacts available to send SOS.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Sending SMS
        val smsManager = SmsManager.getDefault()
        var sentCount = 0
        contactList.forEach { phoneNumber ->
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d("SMSSent", "SMS sent to $phoneNumber")
                sentCount++
            } catch (e: Exception) {
                Log.e("SMSError", "Failed to send SMS to $phoneNumber: ${e.message}")
            }
        }
        contactList.forEach { phoneNumber ->
            sendWhatsAppMessage(phoneNumber, message)
        }

        if (sentCount > 0) {
            Toast.makeText(
                requireContext(),
                "SOS message sent to $sentCount contacts via SMS.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to send SOS message via SMS.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun sendWhatsAppMessage(phoneNumber: String, message: String) {
        try {
            val uri = Uri.parse("smsto:$phoneNumber") // Use WhatsApp protocol
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra("sms_body", message)
            intent.setPackage("com.whatsapp")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(
                "WhatsAppError",
                "Failed to send message to $phoneNumber via WhatsApp: ${e.message}"
            )
        }
    }

    override fun onLocationChanged(location: Location) {
        // You can also choose to send location immediately when it changes
        sendLocation(location)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop location updates and handler to save battery
        locationManager.removeUpdates(this)
        handler.removeCallbacksAndMessages(null) // Clear all callbacks
        isSendingSOS = false
    }

    companion object {
        private const val MIN_TIME = 1000 // Update interval in milliseconds
        private const val MIN_DISTANCE = 10 // Update distance in meters
        private const val LOCATION_PERMISSION_REQUEST_CODE = 44
    }
}
