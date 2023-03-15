package ru.edgecenter.edge_vod.screens.main.video_player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.MediaController
import androidx.fragment.app.Fragment
import edge_vod.R
import edge_vod.databinding.FragmentVideoPlayerBinding

class VideoPlayerFragment : Fragment(R.layout.fragment_video_player) {

    private lateinit var binding: FragmentVideoPlayerBinding
    private var videoUri: String? = null
    private var currentFrame = FIRST_FRAME

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVideoPlayerBinding.bind(view)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        val videoTitle = requireArguments().getString(VIDEO_TITLE_KEY) ?: ""
        videoUri = requireArguments().getString(VIDEO_URI_KEY)
        binding.videoTitleTv.text = videoTitle

        currentFrame = savedInstanceState?.getInt(PLAYBACK_TIME_KEY) ?: FIRST_FRAME
        setSystemUiVisibilityChangeListener()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        // In Android versions higher than P, videoView does not survive before the onSaveInstanceState()
        // method is called, so the current video time must be saved earlier
        currentFrame = binding.videoView.currentPosition

        // In Android versions less than N (7.0, API 24), onPause() is the
        // end of the visual lifecycle of the app.  Pausing the video here
        // prevents the sound from continuing to play even after the app
        // disappears.
        //
        // This is not a problem for more recent versions of Android because
        // onStop() is now the end of the visual lifecycle, and that is where
        // most of the app teardown should take place.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) binding.videoView.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PLAYBACK_TIME_KEY, currentFrame)
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onDestroy() {
        super.onDestroy()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun initializePlayer() {
        val videoView = binding.videoView
        videoView.setVideoURI(Uri.parse(videoUri))

        val mediaController = MediaController(videoView.context)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)

        videoView.setOnPreparedListener {
            videoView.seekTo(currentFrame)
            binding.progressBar.visibility = View.GONE
            videoView.start()
        }
    }

    private fun releasePlayer() {
        binding.videoView.stopPlayback()
    }

    private fun setSystemUiVisibilityChangeListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    hideSystemUI()
                }
            }
        }
    }

    private fun hideSystemUI() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.setDecorFitsSystemWindows(false)
                requireActivity().window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                binding.videoView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.setDecorFitsSystemWindows(true)
                requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            } else {
                binding.videoView.systemUiVisibility = View.VISIBLE
            }
        }
    }

    companion object {
        const val VIDEO_TITLE_KEY = "name"
        const val VIDEO_URI_KEY = "uri"

        private const val PLAYBACK_TIME_KEY = "playTime"
        private const val FIRST_FRAME = 1
    }
}