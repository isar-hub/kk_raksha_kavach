package com.isar.kkrakshakavach.sos

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.isar.kkrakshakavach.databinding.ActivitySosactivityBinding
import com.isar.kkrakshakavach.db.DbClassHelper
import com.isar.kkrakshakavach.utils.CommonMethods
import kotlinx.coroutines.ExperimentalCoroutinesApi


class SOSActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosactivityBinding
    private lateinit var viewModel: SOSViewmodel
    private val cameraViewModel: CameraViewModel by viewModels()


    private var imageCapture: ImageCapture? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DbClassHelper(this)
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val repository = SOSRepository(dbHelper, fusedLocationProviderClient, this)

        val factory = SOSViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SOSViewmodel::class.java]
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), 1)

        CommonMethods.showLogs("SOSActivity", "Activity created and initialized")

        binding.btSendLocation.setOnClickListener {
            CommonMethods.showLogs("SOSActivity", "Send Location button clicked")
            viewModel.isSendingSos.postValue(true)
//            viewModel.fetchLocation(this)
            startCamera()
        }


        viewModel.isSendingSos.observe(this) {
            CommonMethods.showLogs("SOSActivity", "isSendingSos observed with value: $it")
            if (it) {
                setupObservers()
                setupCameraObservers()
            }
            binding.loader.apply {
                if (it) {
                    visibility = View.VISIBLE
                    isEnabled = false
                } else {
                    visibility = View.GONE
                    isEnabled = true
                }
            }
        }
    }

    private fun startCamera() {
        CommonMethods.showLogs("SOSActivity", "Starting camera initialization")

        binding.previewView.visibility = View.VISIBLE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            val videoCapture = VideoCapture.withOutput(recorder)

            cameraViewModel.setImageCapture(imageCapture!!)
            cameraViewModel.setVideoCapture(videoCapture)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview,videoCapture, imageCapture)
                cameraViewModel.startCaptureAndUploadFlow(viewModel, imageCapture!!,this)

                CommonMethods.showLogs("SOSActivity", "Camera successfully initialized")

            } catch (e: Exception) {
                CommonMethods.showLogs("SOSActivity", "Error initializing camera: ${e.message}")

                Toast.makeText(this, "Error initializing camera: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupCameraObservers() {
        CommonMethods.showLogs("SOSActivity", "Setting up camera observers")

        cameraViewModel.photoUri.observe(this) { uri ->
            CommonMethods.showLogs("SOSActivity", "Setting up camera observers")

            Toast.makeText(this, "Photo saved at: $uri", Toast.LENGTH_SHORT).show()
        }

        cameraViewModel.errorMessage.observe(this) { error ->
            CommonMethods.showLogs("SOSActivity", "Error observed: $error")

            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }

        cameraViewModel.isCapturing.observe(this) { isCapturing ->
            CommonMethods.showLogs("SOSActivity", "isCapturing observed: $isCapturing")

            if (isCapturing) {
                Toast.makeText(this, "Capturing photo...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        CommonMethods.showLogs("SOSActivity", "Setting up observers for location updates")

        viewModel.locationLiveData.observe(this) { location ->
            CommonMethods.showLogs("SOSActivity", "Location update observed: $location")

            when (location) {
                is LocationUpdate.Done -> {
                    // Log the location details
                    CommonMethods.showLogs(
                        "SOS",
                        "Location is ${location.location.latitude} and ${location.location.longitude}"
                    )

                    // Append the help message with the location
                    viewModel.appendMessage("I need help! My location is: https://maps.google.com/?q=${location.location.latitude},${location.location.longitude}")

                    // Retrieve the contacts
                    val contacts = viewModel.allContacts.value
                    if (!contacts.isNullOrEmpty()) { // Check // if contacts are not null or empty
                        CommonMethods.showLogs("SOSActivity", "Contacts fetched: $contacts")

                        contacts.forEach { contact ->
                            // Send SMS to each contact
                            viewModel.sendSms(contact, this)
                            CommonMethods.showLogs("SOSActivity", "SMS sent to: $contact")

                        }
                    } else {
                        CommonMethods.showLogs("SOSActivity", "No contacts available")

                        showToast("No contacts available. Please add contacts.")
                        stopLoader()
                    }
                }

                LocationUpdate.GpsNotEnabled -> {
                    stopLoader()
                    showToast("Please enable GPS.")
                }

                LocationUpdate.LocationError -> {
                    CommonMethods.showLogs("SOSActivity", "Location error occurred")

                    stopLoader()
                    showToast("Something went wrong while fetching location.")
                }

                LocationUpdate.PermissionDenied -> {
                    CommonMethods.showLogs("SOSActivity", "Permission denied")

                    stopLoader()
                    showToast("Please allow permission first.")
                }
            }
        }

        // Start location updates (consider moving this to where you initialize your location updates)
//        startLocationUpdates()
    }

    private fun stopLoader() {
        viewModel.isSendingSos.postValue(false)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    }

    private fun startLocationUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val locationRunnable = object : Runnable {
            override fun run() {
                viewModel.fetchLocation(this@SOSActivity)
                handler.postDelayed(this, 20000)
            }
        }
        handler.post(locationRunnable)
    }
    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraViewModel.photoUri.removeObservers(this)
        cameraViewModel.errorMessage.removeObservers(this)
        cameraViewModel.isCapturing.removeObservers(this)
    }

}

