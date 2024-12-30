package com.isar.kkrakshakavach

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.isar.kkrakshakavach.databinding.FragmentSoSBinding
import com.isar.kkrakshakavach.db.DbClassHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class SoSFragment : Fragment(), LocationListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private val handler = Handler()
    private lateinit var myDB: DbClassHelper
    private lateinit var imageCapture: ImageCapture
    private var isSendingSOS = false
    var storage: FirebaseStorage =
        FirebaseStorage.getInstance(FirebaseApp.getInstance("StorageApp"))
    private var message = "";
    private lateinit var list: ArrayList<String>
    private lateinit var binding: FragmentSoSBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSoSBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SoSFragment", "onViewCreated called")


        imageCapture = ImageCapture.Builder().build()
//        surfaceView = binding.previewView


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        myDB = DbClassHelper(requireContext())




        list = getAllContacts()
        Log.d("SoSFragment", "Contacts loaded: ${list.size} contacts found")


        // Set up the SOS button click listener
        view.findViewById<MaterialButton>(R.id.bt_sendLocation).setOnClickListener {
            if (!isSendingSOS) {
                Log.d("SoSFragment", "SOS button clicked")

                lifecycleScope.launch {
                    startSending(contacts = list)
                }
//                startLocationUpdates()
            } else {
                Toast.makeText(requireContext(), "SOS is already in progress.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private suspend fun startSending(contacts: ArrayList<String>) {

        isSendingSOS = true
        binding.loader.visibility = View.VISIBLE

        Log.d("SoSFragment", "Starting to send SOS")
        Log.d("SoSFragment", "Contacts $contacts")

        if (contacts.isNotEmpty()) {

            startLocationUpdates()

        } else {
            Toast.makeText(
                requireContext(),
                "Contacts are empty. Please add contacts first.",
                Toast.LENGTH_LONG
            ).show()
            binding.loader.visibility = View.GONE
            isSendingSOS = false
            Log.w("SoSFragment", "No contacts found, unable to send SOS.")


        }

    }


    private fun startLocationUpdates() {

        Log.d("SoSFragment", "Starting location updates")

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("SoSFragment", "Location permissions not granted")

            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            binding.loader.visibility = View.GONE
            isSendingSOS = false
            Toast.makeText(
                requireContext(),
                "Please enable GPS for location tracking.",
                Toast.LENGTH_LONG
            ).show()
            Log.w("SoSFragment", "GPS is not enabled")

            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("SoSFragment", "Location retrieved: ${it.latitude}, ${it.longitude}")

                message =
                    "I need help! My location is: https://maps.google.com/?q=${it.latitude},${it.longitude}"
                runBlocking {
                    async { startCamera() }.await()

//                    sendMessages(message)
                }
            } ?: run {
                Log.e("LocationError", "Last known location is null, requesting new location.")
                isSendingSOS = false
            }
            requestNewLocationData()
        }.addOnFailureListener {
            Log.e("SoSFragment", "Last known location is null, requesting new location.")

        }

        handler.postDelayed(object : Runnable {
            override fun run() {
                requestNewLocationData()
                handler.postDelayed(this, 60000) // 5 minutes
            }
        }, 60000)
    }

    private fun requestNewLocationData() {
        Log.d("SoSFragment", "Requesting new location data")

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT)
                .show()
            Log.w("SoSFragment", "Location permission denied")

            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("SoSFragment", "New location retrieved: ${it.latitude}, ${it.longitude}")
                message =
                    "I need help! My location is: https://maps.google.com/?q=${it.latitude},${it.longitude}"
                runBlocking {
                    startCamera()
                }
            } ?: Log.e("LocationError", "Unable to retrieve new location.")
        }
    }

    private fun getAllContacts(): ArrayList<String> {
        Log.d("SoSFragment", "Fetching all contacts from database")

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
        return contactList
    }

    private fun getTimeStamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }


    private suspend fun putAudio(audioFile: Uri): Uri? {

        Log.d("SoSFragment", "Uploading audio file: ${audioFile.path}")

        var result: Uri? = null
        val audioRef = storage.reference.child("sos_audio/audio_${getTimeStamp()}.3gp")


        audioRef.putFile(audioFile).addOnSuccessListener {

            audioRef.downloadUrl.addOnSuccessListener { audioUri ->
                result = audioUri
                Log.d("SoSFragment", "Audio uploaded successfully: $audioUri")

                message += "  Audio: $result" // Append audio URI to message
                isSendingSOS = false
                binding.loader.visibility = View.GONE

                sendMessages(message)

            }
        }.addOnFailureListener {
            binding.loader.visibility = View.GONE
            Log.e("FirebaseError", "Failed to upload audio: ${it.message}")
        }
        return result
    }

    private suspend fun putImage(imageFile: Uri): Uri? {
        Log.d("SoSFragment", "Uploading image file: ${imageFile.path}")


        resizeImage(0, 0)
        var result: Uri? = null
        val imageRef = storage.reference.child("sos_images/image_${getTimeStamp()}.jpg")

        // Use Tasks API to handle the upload operation
        try {
            imageRef.putFile(imageFile).await() // Use Kotlin coroutines
            result = imageRef.downloadUrl.await() // Wait for the download URL
            Log.d("SoSFragment", "Image uploaded successfully: $result")
        } catch (e: Exception) {
            binding.loader.visibility = View.GONE
            Log.e("FirebaseError", "Failed to upload image: ${e.message}")
        }

        return result
    }

    fun resizeImage(height: Int, width: Int) {
        val previewView: PreviewView = binding.previewView

        val params = previewView.layoutParams
        params.width = width
        params.height = height
        previewView.layoutParams = params

    }

    private fun startCamera() {
        Log.d("SoSFragment", "Starting camera for image capture")


        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        resizeImage(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@SoSFragment, // Replace with your fragment or activity reference
                    cameraSelector,
                    preview,
                    imageCapture
                )
                lifecycleScope.launch {
                    async { captureCurrentImage() }.await()
                }
            } catch (exc: Exception) {
                Log.e("CameraX", "Binding failed", exc)
            }
            Log.d("SoSFragment", "Camera successfully bound")


        }, ContextCompat.getMainExecutor(requireContext()))


    }
    private fun unbindCamera() {
        Log.d("SoSFragment", "Unbinding camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll() // Unbind all use cases
            binding.previewView.visibility = View.GONE // Optionally hide the preview view
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    private suspend fun captureCurrentImage() {
        Log.d("SoSFragment", "Capturing current image")

        val timeStamp = getTimeStamp()
        val imageFile = File(requireContext().externalCacheDir, "IMG_$timeStamp.jpg")
        val imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )

        // Prepare to capture the image
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        delay(100)
        // Capture the image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Handle the success of the image capture here

                    Log.d("ImageCapture", "Image saved: ${imageFile.absolutePath}")
                    lifecycleScope.launch {
                        val image = async { putImage(imageUri) }.await()
                        message += "  Image: $image" // Use += to concatenate strings
                        unbindCamera()
                        async { captureCurrentAudio() }.await()
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("ImageCaptureError", "Failed to capture image: ${exception.message}")
                }
            }
        )
    }

    private suspend fun captureCurrentAudio() {

        Log.d("SoSFragment", "Starting audio capture")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioFile = File(requireContext().externalCacheDir, "AUDIO_$timeStamp.3gp")

        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            prepare()
        }

        try {
            recorder.start()
            Log.d("SoSFragment", "Audio recording started")

            // Sleep to record 10 seconds of audio (or configure duration as needed)ima
            delay(10000)

            recorder.stop()

            delay(100)
            runBlocking {
                async { putAudio(Uri.fromFile(audioFile)) }.await()


            }
        } catch (e: Exception) {
            Log.e("AudioCaptureError", "Error capturing audio: ${e.message}")

        } finally {
            recorder.release()
        }

    }

    private fun sendMessages(message: String) {
        val deliveryIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            Intent("SMS_DELIVERED"),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Register the broadcast receiver for SMS delivery
        val deliveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK -> Log.d("SMS", "SMS delivered successfully")
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Log.e("SMS", "Generic failure")
                    SmsManager.RESULT_ERROR_NO_SERVICE -> Log.e("SMS", "No service available")
                    SmsManager.RESULT_ERROR_NULL_PDU -> Log.e("SMS", "Null PDU")
                    SmsManager.RESULT_ERROR_RADIO_OFF -> Log.e("SMS", "Radio off")
                    else -> Log.e("SMS", "Unknown error code: $resultCode")
                }
            }
        }
        registerReceiver(
            requireContext(),
            deliveryReceiver,
            IntentFilter("SMS_DELIVERED"),
            ContextCompat.RECEIVER_NOT_EXPORTED // Use RECEIVER_NOT_EXPORTED for security
        )
        Log.d("SoSFragment", "Broadcast receiver registered")

        Log.d("SoSFragment", "Sending messages to contacts message = $message")
        for (contact in list) {
            try {
                SmsManager.getDefault().sendTextMessage(contact, null, message, null, deliveryIntent)
                Log.d("SMS", "Sent message to $contact")
            } catch (e: Exception) {
                Log.e("SMSError", "Failed to send message to $contact: ${e.message}")
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                requireActivity().unregisterReceiver(deliveryReceiver)
            } catch (e: IllegalArgumentException) {
                Log.e("SMS", "Receiver already unregistered: ${e.message}")
            }
        }, 5000)

    }

    override fun onLocationChanged(location: Location) {
//        sendLocation(location)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Cleanup handler callbacks
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Cleanup handler callbacks
        Log.d("SoSFragment", "Fragment destroyed, handler callbacks removed")
    }

    companion object {
        private const val MIN_TIME = 1000
        private const val MIN_DISTANCE = 10
        private const val IMAGE_CAPTURE_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 44
    }
}
