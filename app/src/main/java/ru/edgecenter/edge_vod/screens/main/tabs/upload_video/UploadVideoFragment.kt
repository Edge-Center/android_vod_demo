package ru.edgecenter.edge_vod.screens.main.tabs.upload_video

import android.Manifest
import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import edge_vod.R
import edge_vod.databinding.FragmentUploadVideoBinding
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.UploadingVODsViewModel
import ru.edgecenter.edge_vod.utils.sharedViewModelCreator
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.nanoseconds

class UploadVideoFragment : Fragment(R.layout.fragment_upload_video) {

    private lateinit var binding: FragmentUploadVideoBinding

    private val sharedViewModel: UploadingVODsViewModel by sharedViewModelCreator {
        UploadingVODsViewModel(requireActivity().application)
    }

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var videoRecordEvent: VideoRecordEvent? = null

    private val mainThreadExecutor by lazy {
        ContextCompat.getMainExecutor(requireContext())
    }

    private var outputVideoUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUploadVideoBinding.bind(view)

        if (allPermissionGranted()) {
            startCamera()
        } else {
            requestPermissions.launch(REQUIRED_PERMISSIONS)
        }

        resetUiState()
        configureButtons()
    }

    private fun configureButtons() {
        binding.apply {
            videoCaptureBtn.setOnClickListener {
                if (videoRecordEvent is VideoRecordEvent.Finalize || videoRecordEvent == null) {
                    startRecoding()
                } else {
                    stopRecording()
                }
            }
            playPauseRecord.setOnClickListener {
                if (
                    videoRecordEvent is VideoRecordEvent.Resume
                    || videoRecordEvent is VideoRecordEvent.Start
                ) {
                    recording?.pause()
                }
                if (videoRecordEvent is VideoRecordEvent.Pause) {
                    recording?.resume()
                }
            }
            uploadVideo.setOnClickListener {
                outputVideoUri?.let {
                    showVideoUploadDialog(it)
                }
                resetUiState()
            }
            flipCamera.setOnClickListener {
                flipCamera()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }, mainThreadExecutor)
    }

    private fun startRecoding() {
        val videoCapture = videoCapture ?: return
        binding.videoCaptureBtn.isEnabled = false

        val name =
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }
        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            requireActivity().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording =
            videoCapture.output.prepareRecording(requireContext(), mediaStoreOutputOptions).apply {
                val audioPermission = PermissionChecker.checkSelfPermission(
                    requireContext(), Manifest.permission.RECORD_AUDIO
                )
                if (audioPermission == PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }.start(mainThreadExecutor, videoRecordEventListener)
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun showVideoUploadDialog(localVideoUri: Uri) {
        val dialogButton = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                sharedViewModel.uploadVideo(localVideoUri)
                Toast.makeText(requireContext(), R.string.video_sent_to_server, Toast.LENGTH_SHORT)
                    .show()
            }
            dialog.dismiss()
        }
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle(R.string.upload_video_dialog_title)
            .setPositiveButton(R.string.yes, dialogButton)
            .setNegativeButton(R.string.no, dialogButton)
            .create()
            .show()
    }

    private val videoRecordEventListener = Consumer { event: VideoRecordEvent ->
        if (event !is VideoRecordEvent.Status) {
            videoRecordEvent = event
        }
        updateUI(event)
    }

    private fun updateUI(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Status -> {
                val timeInNanos = event.recordingStats.recordedDurationNanos
                val time = timeInNanos.nanoseconds.toComponents { hours, minutes, seconds, _ ->
                    TIME_FORMAT.format(hours, minutes, seconds)
                }
                binding.recordedDuration.text = time
            }
            is VideoRecordEvent.Start -> {
                setStartRecordingUIState()
            }
            is VideoRecordEvent.Pause -> {
                binding.playPauseRecord.setImageResource(R.drawable.ic_play_24)
            }
            is VideoRecordEvent.Resume -> {
                binding.playPauseRecord.setImageResource(R.drawable.ic_pause_24)
            }
            is VideoRecordEvent.Finalize -> {
                if (!event.hasError()) {
                    outputVideoUri = event.outputResults.outputUri
                    Toast.makeText(
                        requireContext(),
                        R.string.video_capture_succeeded,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    recording?.close()
                    recording = null
                    Log.e(TAG, "${event.error}")
                }
                setStopRecordingUIState()
            }
        }
    }

    private fun resetUiState() {
        binding.apply {
            recordedDuration.text = ""
            recordedDuration.visibility = View.GONE

            videoCaptureBtn.setImageResource(R.drawable.ic_red_circle_24)
            videoCaptureBtn.isEnabled = true

            playPauseRecord.isEnabled = false
            playPauseRecord.setImageResource(R.drawable.ic_pause_24)
            playPauseRecord.visibility = View.GONE

            flipCamera.isEnabled = true
            flipCamera.visibility = View.VISIBLE

            uploadVideo.isEnabled = false
            uploadVideo.visibility = View.GONE
        }
    }

    private fun setStartRecordingUIState() {
        binding.apply {
            recordedDuration.visibility = View.VISIBLE

            videoCaptureBtn.setImageResource(R.drawable.ic_round_stop_24)
            videoCaptureBtn.isEnabled = true

            flipCamera.isEnabled = false
            flipCamera.visibility = View.GONE

            playPauseRecord.isEnabled = true
            playPauseRecord.visibility = View.VISIBLE
            playPauseRecord.setImageResource(R.drawable.ic_pause_24)

            uploadVideo.isEnabled = false
            uploadVideo.visibility = View.GONE
        }
    }

    private fun setStopRecordingUIState() {
        binding.apply {
            videoCaptureBtn.setImageResource(R.drawable.ic_red_circle_24)
            videoCaptureBtn.isEnabled = true

            playPauseRecord.isEnabled = false
            playPauseRecord.visibility = View.GONE

            flipCamera.isEnabled = true
            flipCamera.visibility = View.VISIBLE

            uploadVideo.isEnabled = true
            uploadVideo.visibility = View.VISIBLE
        }
    }

    private fun flipCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext().applicationContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (allPermissionGranted()) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "UploadVideoFragment"
        private const val TIME_FORMAT = "%02d:%02d:%02d"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}