package com.isar.kkrakshakavach.sos

import android.Manifest
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_UNKNOWN
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.isar.kkrakshakavach.utils.CommonMethods
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private var storage =
        FirebaseStorage.getInstance(FirebaseApp.getInstance("StorageApp")).reference
    private val appContext = application.applicationContext

    val photoUri = MutableLiveData<Uri>()
    val errorMessage = MutableLiveData<String>()
    val isCapturing = MutableLiveData<Boolean>()

    //    private var mediaRecorder: MediaRecorder? = null
//    private var audioFile: File? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
    }

    fun setVideoCapture(capture: VideoCapture<Recorder>) {
        videoCapture = capture
    }

    fun startAutoCapture(delayMillis: Long) {
        isCapturing.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            delay(delayMillis)
            capturePhoto()
            isCapturing.postValue(false)
        }
    }

    private fun generateUniqueFilename(): String {
        val randomPart = (1..9).random()
            .toString() + (0 until (4..9).random()).joinToString("") { (0..9).random().toString() }
        val timestampPart = System.currentTimeMillis().toString()
        return (randomPart + timestampPart).take(8)
    }

    private suspend fun capturePhoto(): Uri {

        return suspendCancellableCoroutine { continuation ->

            try {
                val photoFile = File(
                    appContext.externalCacheDir, generateUniqueFilename() + ".jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture?.takePicture(outputOptions,
                    Dispatchers.IO.asExecutor(),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val photoUri = Uri.fromFile(photoFile)
                            CommonMethods.showLogs("CAMERA", "Photo captured: $photoUri")
                            continuation.resume(photoUri)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            CommonMethods.showLogs(
                                "CAMERA", "ror capturing photo: ${exception.message}"
                            )
                            continuation.resumeWithException(exception)
                        }
                    })
                    ?: continuation.resumeWithException(IllegalStateException("ImageCapture is null"))
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }

        }
    }

    fun startCaptureAndUploadFlow(
        viewModel: SOSViewmodel, imageCapture: ImageCapture, context: Context
    ) {
        this.imageCapture = imageCapture

        viewModelScope.launch(Dispatchers.IO) {
            try {
                CommonMethods.showLogs("CAMERA", "arting capture and upload flow")


                // Capture Video
                val videoUri = recordVideo()
                CommonMethods.showLogs("Camer","Video Get")
                val videoUrl = uploadToFirebase(videoUri, "videos/")
                viewModel.appendMessage("Video URL: $videoUrl")
                CommonMethods.showLogs("CAMERA", "Video uploaded: $videoUrl")

                // Capture Photo
                val imageUri = capturePhoto()
                val imageUrl = uploadToFirebase(imageUri, "images/")
                viewModel.appendMessage("Image URL: $imageUrl")
                CommonMethods.showLogs("CAMERA", "age uploaded: $imageUrl")


                // Send SMS
                viewModel.sendCamerasSms(context)
                CommonMethods.showLogs("CAMERA", "SOS sent successfully")
            } catch (e: Exception) {
                CommonMethods.showLogs("CAMERA", "Error in capture and upload flow: ${e.message}")
                errorMessage.postValue(e.message)
            }
        }
    }

    private suspend fun recordVideo(): Uri {
        return suspendCancellableCoroutine { continuation ->
            try {

                val contentResolver = appContext.contentResolver
//                val collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "${generateUniqueFilename()}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                }
//                val videoUri = contentResolver.insert(collectionUri, contentValues)
//                if (videoUri == null) {
//                    continuation.resumeWithException(Exception("Failed to create MediaStore entry. URI is null."))
//                    return@suspendCancellableCoroutine
//                }
//                else{
//                    CommonMethods.showLogs("SOS","$videoUri")
//                }



                val mediaStoreOutputOptions = MediaStoreOutputOptions
                    .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(contentValues)
                    .build()


                if (activeRecording != null) {
                    throw Exception("Recording is already in progress.")
                }
                if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    continuation.resumeWithException(Exception("Audio recording permission not granted"))
                    return@suspendCancellableCoroutine
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(
                            appContext, Manifest.permission.READ_MEDIA_VIDEO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        continuation.resumeWithException(
                            Exception("Media access permission not granted for Android 13+")
                        )
                        return@suspendCancellableCoroutine
                    }
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        continuation.resumeWithException(
                            Exception("External Storage permission not granted")
                        )
                        return@suspendCancellableCoroutine
                    }
                }



                activeRecording = videoCapture!!.output.prepareRecording(appContext, mediaStoreOutputOptions)
                    .withAudioEnabled()
                    .start(Dispatchers.IO.asExecutor()) { recordEvent ->

                        when (recordEvent) {
                            is VideoRecordEvent.Status ->{
//                                CommonMethods.showLogs("CAMERA","STATUS = ${recordEvent.recordingStats}")
                            }
                            is VideoRecordEvent.Resume ->{

                            }
                            is VideoRecordEvent.Finalize -> {
                                CommonMethods.showLogs("CAMERA", "Video recording finalized")

                                if (recordEvent.hasError()) {

                                    continuation.resumeWithException(Exception("Error recording video: ${recordEvent.error} ${recordEvent.outputResults}"))
//                                    when (recordEvent.error) {
//                                        ERROR_UNKNOWN, ERROR_RECORDER_ERROR, ERROR_ENCODING_FAILED, ERROR_NO_VALID_DATA -> {
//                                            if (recordEvent.outputOptions is FileOutputOptions) {
//                                                try {
//                                                    (recordEvent.outputOptions as FileOutputOptions).file.delete()
//                                                    CommonMethods.showLogs("VIDEO_CAPTURE", "File deleted for FileOutputOptions.")
//                                                } catch (e: Exception) {
//                                                    CommonMethods.showLogs("VIDEO_CAPTURE", "Failed to delete file for FileOutputOptions: ${e.message}")
//                                                }
//                                            } else if (recordEvent.outputOptions is MediaStoreOutputOptions) {
//                                                val uri = recordEvent.outputResults.outputUri
//                                                if (uri !== Uri.EMPTY) {
//                                                    try {
//                                                        appContext.contentResolver.delete(uri, null, null)
//                                                        CommonMethods.showLogs("VIDEO_CAPTURE", "MediaStore entry deleted for URI: $uri")
//                                                    } catch (e: Exception) {
//                                                        CommonMethods.showLogs("VIDEO_CAPTURE", "Failed to delete MediaStore entry for URI: $uri, error: ${e.message}")
//                                                    }
//                                                } else {
//                                                    CommonMethods.showLogs("VIDEO_CAPTURE", "MediaStore URI is empty, nothing to delete.")
//                                                }
//                                            } else if (recordEvent.outputOptions is FileDescriptorOutputOptions) {
//                                                CommonMethods.showLogs("VIDEO_CAPTURE", "Unhandled case: FileDescriptorOutputOptions.")
//                                            } else {
//                                                CommonMethods.showLogs("VIDEO_CAPTURE", "Unhandled outputOptions type: ${recordEvent.outputOptions::class.simpleName}")
//                                            }
//                                        }
//                                        else -> {
//                                            CommonMethods.showLogs("VIDEO_CAPTURE", "Unhandled error code: ${recordEvent.error}")
//                                        }
//                                    }
                                } else {
                                    continuation.resume(recordEvent.outputResults.outputUri)
                                }
                            }

                        }
                    }

                CoroutineScope(Dispatchers.IO).launch {
                    delay(15000) // Delay for recording duration
                    activeRecording?.stop() // Stop recording after delay
//                    ProcessCameraProvider.getInstance(appContext).
                }

                CommonMethods.showLogs("CAMERA", "Video recording started")

            } catch (e: Exception) {
                CommonMethods.showLogs("CAMERA", "Error recording video: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }

    private fun stopVideoRecording() {
        try {
            activeRecording?.stop()
            activeRecording = null
            CommonMethods.showLogs("TAG","$activeRecording  $videoCapture")

            CommonMethods.showLogs("CAMERA", "Video recording stopped")
        } catch (e: Exception) {
            CommonMethods.showLogs("CAMERA", "Error stopping video recording: ${e.message}")
            errorMessage.postValue("Error stopping video recording: ${e.message}")
        } finally {


        }
    }

//    private suspend fun recordAudio(): Uri {
//        return suspendCancellableCoroutine { continuation ->
//            try {
//                val audioFile = File(
//                    appContext.externalCacheDir,
//                    generateUniqueFilename() + ".m4a"
//                )
//
//                this.audioFile = audioFile
//                mediaRecorder = MediaRecorder().apply {
//                    setAudioSource(MediaRecorder.AudioSource.MIC)
//                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//                    setOutputFile(audioFile.absolutePath)
//                    prepare()
//                    start()
//                }
//
//                CommonMethods.showLogs("CAMERA", "dio recording started")
//
//                // Simulate a 5-second audio recording
//                viewModelScope.launch(Dispatchers.IO) {
//                    delay(5000)
//                    stopAudioRecording()
//                    CommonMethods.showLogs("CAMERA", "dio recording stopped: $audioFile")
//                    continuation.resume(Uri.fromFile(audioFile), null)
//                }
//            } catch (e: Exception) {
//                CommonMethods.showLogs("CAMERA", "ror recording audio: ${e.message}")
//                continuation.resumeWithException(e)
//            }
//        }
//    }
//
//    private fun stopAudioRecording() {
//        try {
//            mediaRecorder?.apply {
//                stop()
//                release()
//            }
//        } catch (e: Exception) {
//            CommonMethods.showLogs("CAMERA", "ror stopping audio recording: ${e.message}")
//            errorMessage.postValue("Error stopping audio recording: ${e.message}")
//        } finally {
//            mediaRecorder = null
//        }
//    }

    private suspend fun uploadToFirebase(fileUri: Uri, folder: String): String {
        val fileName = fileUri.lastPathSegment ?: System.currentTimeMillis().toString()
        val fileRef = storage.child("$folder$fileName")

        return fileRef.putFile(fileUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                CommonMethods.showLogs("CAMERA", "ror uploading file: ${task.exception?.message}")
                throw task.exception ?: Exception("Unknown error occurred while uploading")
            }
            fileRef.downloadUrl
        }.await().toString().also {
            CommonMethods.showLogs("CAMERA", "le uploaded successfully: $it")
        }
    }
}
