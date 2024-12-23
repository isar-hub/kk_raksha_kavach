package com.isar.kkrakshakavach

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.storage.FirebaseStorage
import com.isar.kkrakshakavach.db.DbClassHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SoSFragment : Fragment(), LocationListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var loader : ProgressBar
    private val handler = Handler()
    private lateinit var myDB: DbClassHelper
    private var isSendingSOS = false
    private val firebaseStorage = FirebaseStorage.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_so_s, container, false)
        loader = view.findViewById(R.id.loader)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check and request SMS and Storage permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 1
            )
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2
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
                loader.visibility = View.VISIBLE
                startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "SOS is already in progress.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
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
            isSendingSOS = false
            Toast.makeText(
                requireContext(), "Please enable GPS for location tracking.", Toast.LENGTH_LONG
            ).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                sendLocation(it)
            } ?: run {
                Log.e("LocationError", "Last known location is null, requesting new location.")
                isSendingSOS = false
            }
            requestNewLocationData()
        }

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
            Toast.makeText(requireContext(), "com.isar.kkrakshakavach.permissions.Permission Denied", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                sendLocation(it)
            } ?: Log.e("LocationError", "Unable to retrieve new location.")
        }
    }

    private fun sendLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val message = "I need help! My location is: https://maps.google.com/?q=$lat,$lon"

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
                requireContext(), "No contacts available to send SOS.", Toast.LENGTH_SHORT
            ).show()
            isSendingSOS = false
            return
        }

        uploadMediaFiles(contactList, message)
    }

    private fun uploadMediaFiles(contactList: ArrayList<String>, message: String) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // Simulated file paths (replace with actual file paths)
        val imagePath = captureCurrentImage() // Implement this method to capture an image using the camera
        val audioPath = captureCurrentAudio() // Implement this method to record audio
        if (imagePath == null || audioPath == null) {
            Log.e("MediaCaptureError", "Failed to capture image or audio.")
            Toast.makeText(requireContext(), "Unable to capture media.", Toast.LENGTH_SHORT).show()
            return
        }
        val audioRef = firebaseStorage.reference.child("sos_audio/audio_$timestamp.3gp")
        val imageRef = firebaseStorage.reference.child("sos_images/image_$timestamp.jpg")

        val audioFile = Uri.fromFile(File(audioPath))
        val imageFile = Uri.fromFile(File(imagePath))

        audioRef.putFile(audioFile).addOnSuccessListener {
            audioRef.downloadUrl.addOnSuccessListener { audioUri ->
                imageRef.putFile(imageFile).addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { imageUri ->
                        val fullMessage = "$message\nAudio: $audioUri\nImage: $imageUri"
                        sendMessages(contactList, fullMessage)
                    }
                }.addOnFailureListener {
                    loader.visibility = View.GONE
                    Log.e("FirebaseError", "Failed to upload image: ${it.message}")
                }
            }
        }.addOnFailureListener {
            loader.visibility = View.GONE

            Log.e("FirebaseError", "Failed to upload audio: ${it.message}")
        }
    }

    private fun captureCurrentImage(): String? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(requireContext().externalCacheDir, "IMG_$timeStamp.jpg")
        val imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", imageFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        try {
            startActivityForResult(intent, IMAGE_CAPTURE_REQUEST_CODE)
            return imageFile.absolutePath
        } catch (e: Exception) {
            loader.visibility = View.GONE

            Log.e("ImageCaptureError", "Failed to capture image: ${e.message}")
        }
        return null
    }

    private fun captureCurrentAudio(): String? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioFile = File(requireContext().externalCacheDir, "AUDIO_$timeStamp.3gp")

        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
        }


        try {
            recorder.prepare()
            recorder.start()

            // Sleep to record 10 seconds of audio (or configure duration as needed)ima
            Thread.sleep(10000)

            recorder.stop()
            recorder.release()
            return audioFile.absolutePath
        } catch (e: Exception) {
            loader.visibility = View.GONE
 
            Log.e("AudioCaptureError", "Failed to capture audio: ${e.message}")
        }

        return null
    }

    private fun sendMessages(contactList: ArrayList<String>, message: String) {
        val smsManager = SmsManager.getDefault()
        var sentCount = 0

        contactList.forEach { phoneNumber ->
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d("SMSSent", "SMS sent to $phoneNumber")
                sentCount++
            } catch (e: Exception) {
                loader.visibility = View.GONE

                Log.e("SMSError", "Failed to send SMS to $phoneNumber: ${e.message}")
            }
        }

        if (sentCount > 0) {
            loader.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "SOS message sent to $sentCount contacts.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            loader.visibility = View.GONE
            Toast.makeText(
                requireContext(), "Failed to send SOS message.", Toast.LENGTH_LONG
            ).show()
        }

        isSendingSOS = false
    }

    override fun onLocationChanged(location: Location) {
        sendLocation(location)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationManager.removeUpdates(this)
        handler.removeCallbacksAndMessages(null)
        isSendingSOS = false
    }

    companion object {
        private const val MIN_TIME = 1000
        private const val MIN_DISTANCE = 10
        private const val IMAGE_CAPTURE_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 44
    }
}
