package com.keepervision.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface.ROTATION_90
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.GsonBuilder
import com.keepervision.R
import com.keepervision.databinding.FragmentCameraBinding
import com.keepervision.utils.auth.auth
import com.keepervision.utils.datatypes.PredictionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class CameraFragment : Fragment() {
    private val TAG: String = "Image Capture"
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var _binding: FragmentCameraBinding? = null
    private lateinit var imageCapture: ImageCapture
    private var captureHandler = Handler(Looper.getMainLooper())
    private lateinit var captureRunnable: Runnable
    private var isCapturing = false

    // Sound related variables
    private var mMediaPlayer: MediaPlayer? = null
    private var mLastAudioPlayed: Int = -1

    // Metrics Collection
    private var mMetricsMap: MutableMap<String, Int>? = null

    private val binding get() = _binding!!
    private val permissionsList: Array<String> = arrayOf(
        Manifest.permission.CAMERA
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val cameraViewModel =
            ViewModelProvider(this).get(CameraViewModel::class.java)

        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val permissionsButton: Button = binding.permissionsButton
        val playButton: ImageButton = binding.playButton
        playButton.setOnClickListener {
            takePicture()
        }
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            permissionsButton.setOnClickListener {
                requestPermissions()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        captureHandler = Handler(Looper.getMainLooper())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDestroyView() {
        super.onDestroyView()
        stopCapture()
        _binding = null
    }

    // App Permission Functions
    private fun requestPermissions() {
        val permissionsToRequest = permissionsList.filter {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        requestPermissions(permissionsToRequest, 100)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (i in permissions.indices) {
            val permissionName = permissionsList[i].substringAfterLast(".")
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    requireContext(),
                    "$permissionName permission granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "$permissionName permission denied. Grant permission in settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val allPermissionsGranted = permissionsList.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            binding.permissionsButton.visibility = View.GONE
            binding.playButton.visibility = View.VISIBLE
        } else {
            binding.permissionsButton.visibility = View.VISIBLE
            binding.playButton.visibility = View.GONE
        }
        return allPermissionsGranted
    }

    // Camera Functions
    private fun startCamera() {
        try {
            imageCapture = ImageCapture.Builder().setTargetRotation(ROTATION_90).build()
            cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                    Log.d(TAG, "Camera Binded Successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting camera provider", e)
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera provider", e)
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK) // Use back camera
            .build()
        val cameraView: PreviewView? = view?.findViewById(R.id.cameraView)
        cameraView?.let { nonNullCameraView ->
            preview.setSurfaceProvider(nonNullCameraView.surfaceProvider)
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takePicture() {
        if (isCapturing) {
            stopCapture()
            binding.playButton.setImageResource(android.R.drawable.ic_media_play)
        } else {
            startCapture()
            binding.playButton.setImageResource(android.R.drawable.ic_media_pause)

            mMediaPlayer = null
            mLastAudioPlayed = -1
            mMetricsMap = mutableMapOf<String, Int>()
        }
    }

    private fun startCapture() {
        var initialImage: File? = null;
        var finalImage : File? = null;
        if (isCapturing) {
            return
        }
        isCapturing = true

        val photosDir = File(requireContext().filesDir, "photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val startTime = LocalDateTime.now().format(formatter).toString()
        captureRunnable = Runnable {
            if(!isCapturing) return@Runnable
            val imageCapture = imageCapture ?: return@Runnable

            val photoFile = File(
                photosDir,
                "captured_image_${System.currentTimeMillis()}.jpg"
            )
            if (initialImage == null) {
                initialImage = photoFile
            };
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            val url = URL(getString(R.string.server_address) + getString(R.string.predict_route))
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        Log.d(TAG, "Image saved: $savedUri")

                        // POST Request
                        val connection = url.openConnection() as HttpURLConnection
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), photoFile)
                                val requestBody = MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("image", photoFile.name, requestFile)
                                    .build()

                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url(url)
                                    .post(requestBody)
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    @RequiresApi(Build.VERSION_CODES.Q)
                                    override fun onResponse(call: Call, response: Response) {
                                        val responseData = response.body?.string()
                                        if(response.code == 200 && isCapturing) {
                                            Log.d(TAG, "$responseData")
                                            // Parsing the JSON response
                                            val gson = GsonBuilder().setLenient().create()
                                            val data = gson.fromJson(responseData, PredictionResponse::class.javaObjectType)
                                            Log.d(TAG, "$responseData")
                                            if(data.idx != 0) {
                                                captureHandler.post(captureRunnable)
                                            }
                                            else {
                                                binding.playButton.setImageResource(android.R.drawable.ic_media_play)
                                                stopCapture()
                                                finalImage = photoFile;
                                                val url = URL(getString(R.string.server_address) + getString(R.string.session_route) + "/" + auth.getUserEmail()!!)

                                                val initialImageRequest = RequestBody.create("image/*".toMediaTypeOrNull(), initialImage!!)
                                                val finalImageRequest = RequestBody.create("image/*".toMediaTypeOrNull(), finalImage!!)

                                                val requestJsonObject= JSONObject()
                                                requestJsonObject.put("f", mMetricsMap!!.getOrDefault("f", 0))
                                                requestJsonObject.put("fl", mMetricsMap!!.getOrDefault("fl", 0))
                                                requestJsonObject.put("fr", mMetricsMap!!.getOrDefault("fr", 0))
                                                requestJsonObject.put("l", mMetricsMap!!.getOrDefault("l", 0))
                                                requestJsonObject.put("r", mMetricsMap!!.getOrDefault("r", 0))
                                                requestJsonObject.put("b", mMetricsMap!!.getOrDefault("b", 0))
                                                requestJsonObject.put("bl", mMetricsMap!!.getOrDefault("bl", 0))
                                                requestJsonObject.put("br", mMetricsMap!!.getOrDefault("br", 0))
                                                requestJsonObject.put("session_start", startTime)
                                                requestJsonObject.put("session_end", LocalDateTime.now().format(formatter).toString())

                                                val requestBody = MultipartBody.Builder()
                                                    .setType(MultipartBody.FORM)
                                                    .addFormDataPart("initial_image", initialImage!!.name, initialImageRequest)
                                                    .addFormDataPart("final_image", finalImage!!.name, finalImageRequest)
                                                    .addFormDataPart("username", auth.getUserEmail()!!)
                                                    .addFormDataPart("session_stats", requestJsonObject.toString())
                                                    .build()

                                                val client = OkHttpClient()
                                                val request = Request.Builder()
                                                    .url(url)
                                                    .post(requestBody)
                                                    .build()

                                                client.newCall(request).enqueue(object : okhttp3.Callback {
                                                    @RequiresApi(Build.VERSION_CODES.Q)
                                                    override fun onResponse(call: Call, response: Response) {
                                                        val responseData = response.body?.string()
                                                        Log.d(TAG, "Session Post API Response: $responseData")
                                                        initialImage!!.delete()
                                                        finalImage!!.delete()
                                                        val photosDir = File(requireContext().filesDir, "photos")
                                                        photosDir.delete()
                                                        requireActivity().runOnUiThread { Toast.makeText(requireContext(), "Session Data Sent", Toast.LENGTH_LONG).show() }
                                                    }

                                                    override fun onFailure(call: Call, e: IOException) {
                                                        Log.e(TAG, "Session Post API request was unsuccessful ${e.message}")
                                                        val photosDir = File(requireContext().filesDir, "photos")
                                                        initialImage!!.delete()
                                                        finalImage!!.delete()
                                                        photosDir.delete()
                                                        requireActivity().runOnUiThread { Toast.makeText(requireContext(), "Session Data Send Failed", Toast.LENGTH_LONG).show() }
                                                    }
                                                })
                                            }
                                            playInstructions(data.idx)

                                            val mapStringEntry = when (data.idx) {
                                                1 -> "f"
                                                2 -> "b"
                                                3 -> "l"
                                                4 -> "fl"
                                                5 -> "bl"
                                                6 -> "r"
                                                7 -> "fr"
                                                8 -> "br"
                                                else -> {
                                                    "done"
                                                }
                                            }
                                            mMetricsMap!![mapStringEntry] = mMetricsMap!!.getOrDefault(mapStringEntry, 0) + 1
                                        }
                                        else {
                                            captureHandler.post(captureRunnable)
                                            Log.e(TAG, "Response not 200 OK or session ended. Returned Status: ${response.code} isCapturing: $isCapturing")
                                        }
                                        if(photoFile != initialImage && photoFile != finalImage) {
                                            photoFile.delete()
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {
                                        Log.e(TAG, "Request was unsuccessful ${e.message}")
                                    }
                                })
                            } catch (e: Exception) {
                                Log.e(TAG, "Exception: ", e)
                            } finally {
                                connection.disconnect()
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.d("Image Capture", "Image capture failed: ${exception.message}", exception)
                        isCapturing = false
                    }
                }
            )
        }

        captureHandler.post(captureRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun stopCapture() {
        try {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
        } catch (e: Exception) {
        }

        if (!isCapturing) {
            return
        }
        isCapturing = false

        // Remove the capture callback only if it is currently posted
        captureHandler.post {
            captureHandler.removeCallbacks(captureRunnable)
        }
        Log.d("Image Capture", "Stop")
        // requireActivity().runOnUiThread { Toast.makeText(requireContext(), mMetricsMap.toString(), Toast.LENGTH_LONG).show() }
    }

    private fun playInstructions (audioReceived: Int) {
//        if (audioReceived != mLastAudioPlayed) { // might need later
        mLastAudioPlayed = audioReceived
        try {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
        } catch (e: Exception) {
        }

        mMediaPlayer = when (audioReceived) {
            0 -> MediaPlayer.create(requireContext(), R.raw.done)
            1 -> MediaPlayer.create(requireContext(), R.raw.front)
            2 -> MediaPlayer.create(requireContext(), R.raw.back)
            3 -> MediaPlayer.create(requireContext(), R.raw.left)
            4 -> MediaPlayer.create(requireContext(), R.raw.front_left)
            5 -> MediaPlayer.create(requireContext(), R.raw.back_left)
            6 -> MediaPlayer.create(requireContext(), R.raw.right)
            7 -> MediaPlayer.create(requireContext(), R.raw.front_right)
            else -> {
                MediaPlayer.create(requireContext(), R.raw.back_right)
            }
        }
        mMediaPlayer!!.start()
    }
}