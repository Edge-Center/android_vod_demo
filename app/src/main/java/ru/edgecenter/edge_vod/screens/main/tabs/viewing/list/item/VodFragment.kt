package ru.edgecenter.edge_vod.screens.main.tabs.viewing.list.item

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
import edge_vod.R
import edge_vod.databinding.FragmentVodBinding
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.UiVideoItem
import ru.edgecenter.edge_vod.screens.main.tabs.viewing.list.ScrollDirection
import ru.edgecenter.edge_vod.screens.main.tabs.viewing.list.ViewingFragment
import ru.edgecenter.edge_vod.utils.viewModelCreator
import kotlin.math.abs

class VodFragment : Fragment(R.layout.fragment_vod) {

    private val viewModel by viewModelCreator {
        VodViewModel(requireActivity().application, videoItemData)
    }

    private lateinit var binding: FragmentVodBinding

    private val edgeApp: ru.edgecenter.edge_vod.EdgeApp
        get() = requireParentFragment().requireActivity().application as ru.edgecenter.edge_vod.EdgeApp

    private val exoPlayer: ExoPlayer?
        get() {
            return if (parentFragment != null) {
                if (position % 2 == 0) {
                    (parentFragment as ViewingFragment).exoPlayer2
                } else {
                    (parentFragment as ViewingFragment).exoPlayer1
                }
            } else {
                null
            }
        }

    // Prepared MediaItem according to current bandwidth
    private var preparedMediaItem: MediaItem? = null
    private var videoItemData: UiVideoItem? = null

    // Position of this fragment in ViewPager2
    private var position: Int = -2

    // Needed to remember the seekPoint of the player on the previous video
    private var seekPoint: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        position = requireArguments().getInt(KEY_POSITION)
        videoItemData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(KEY_VIDEO_DATA, UiVideoItem::class.java)
        } else {
            requireArguments().getParcelable(KEY_VIDEO_DATA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listenScrollDirection()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVodBinding.bind(view)
        initUI()
        configureButtons()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isLoadingVideo) {
            viewModel.cancelPreCacheVideo()
        }
        if (exoPlayer?.playWhenReady == false && preparedMediaItem != null) {
            exoPlayer?.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        seekPoint = exoPlayer?.currentPosition ?: 0
        exoPlayer?.playWhenReady = false
        binding.iconPlay.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        preparedMediaItem = null
        exoPlayer?.removeListener(playerListener)
        super.onDestroyView()
    }

    private fun listenScrollDirection() {
        (requireParentFragment() as ViewingFragment)
            .scrollDirection.observe(viewLifecycleOwner) { direction: ScrollDirection ->
                val offset = abs(direction.displayedPosition - position)
                if (offset > 1) {
                    seekPoint = 0
                    exoPlayer?.removeListener(playerListener)
                    binding.videoPreview.visibility = View.VISIBLE
                }

                if (offset <= ViewingFragment.fragmentsVisibilityScope) {
                    val isDisplayedFragment = position == direction.displayedPosition
                    val isNextFragment = position > direction.displayedPosition
                    val isPrevFragment = position < direction.displayedPosition

                    if (isDisplayedFragment) {
                        if (preparedMediaItem == null) {
                            exoPlayer?.playWhenReady = false
                            viewModel.prepareMediaItem {
                                preparedMediaItem = it
                                preparePlayer(it)
                            }
                        } else {
                            preparePlayer(preparedMediaItem!!)
                        }
                    } else {
                        exoPlayer?.playWhenReady = false
                        binding.playerView.player = null
                    }

                    when (direction) {
                        is ScrollDirection.Forward -> {
                            if (isNextFragment) {
                                precacheVideo()
                            }
                        }
                        is ScrollDirection.Back -> {
                            if (isPrevFragment) {
                                precacheVideo()
                            }
                        }
                    }
                }
            }
    }

    private fun initUI() {
        var videoRatio = 1.0
        videoItemData?.let {
            videoRatio = it.originWidth / it.originHeight.toDouble()
        }

        if (videoRatio !in centerCropRatiosRange) {
            binding.videoPreview.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.playerView.resizeMode = RESIZE_MODE_FIXED_WIDTH
        }

        Glide.with(this)
            .load(Uri.parse(videoItemData?.previewUri))
            .into(binding.videoPreview)


        val currentVolume = (requireParentFragment() as ViewingFragment).currentVolume

        binding.apply {
            videoName.text = videoItemData?.name
            videoId.text = getString(R.string.id, videoItemData?.id)
            playerView.setKeepContentOnPlayerReset(true)

            updateVolumeBtn(currentVolume.value!!)
            amountLikes.text = (100..500).random().toString()
            amountMessages.text = (20..60).random().toString()
            amountShares.text = (1..20).random().toString()
        }

        currentVolume.observe(viewLifecycleOwner) {
            exoPlayer?.volume = it
            updateVolumeBtn(it)
        }
    }

    private fun updateVolumeBtn(volume: Float) {
        val resId = if (volume == 0f) {
            R.drawable.ic_volume_mute
        } else {
            R.drawable.ic_volume_on
        }
        binding.volumeBtn.setImageResource(resId)
    }

    private fun configureButtons() {
        binding.apply {
            playerView.setOnClickListener {
                exoPlayer?.let {
                    it.playWhenReady = !it.isPlaying
                }
            }

            volumeBtn.setOnClickListener {
                exoPlayer?.let {
                    if (it.volume == 0f) {
                        it.volume = 1f
                    } else {
                        it.volume = 0f
                    }
                }
            }
        }
    }

    private fun preparePlayer(mediaItem: MediaItem) {
        val hlsMediaSource = HlsMediaSource.Factory(edgeApp.exoPlayerUtils.cacheDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(mediaItem)

        exoPlayer?.let {
            it.setMediaSource(hlsMediaSource)
            it.prepare()
            it.seekTo(seekPoint)

            if (binding.playerView.player == null) {
                binding.playerView.player = it
            }
            it.playWhenReady = true
            it.addListener(playerListener)
        }
    }

    private val playerListener = object : Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                binding.videoPreview.visibility = View.INVISIBLE
                binding.iconPlay.visibility = View.INVISIBLE
            } else if (exoPlayer?.currentPosition == 0L) {
                binding.videoPreview.visibility = View.VISIBLE
            } else {
                binding.iconPlay.visibility = View.VISIBLE
            }
            super.onIsPlayingChanged(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            binding.progressIndicator.visibility = if (playbackState == Player.STATE_READY) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            super.onPlaybackStateChanged(playbackState)
        }
    }

    private fun precacheVideo() {
        if (!viewModel.isLoadingVideo) {
            if (preparedMediaItem == null) {
                viewModel.prepareMediaItem {
                    preparedMediaItem = it
                    viewModel.startPreCacheVideo(it)
                }
            } else {
                viewModel.startPreCacheVideo(preparedMediaItem!!)
            }
        }
    }

    companion object {
        fun newInstance(videoItem: UiVideoItem, position: Int) = VodFragment()
            .apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_VIDEO_DATA, videoItem)
                    putInt(KEY_POSITION, position)
                }
            }

        private const val KEY_VIDEO_DATA = "KEY_VIDEO_DATA"
        private const val KEY_POSITION = "KEY_POSITION"

        // width/height ratio
        private val centerCropRatiosRange = (0.5..0.6)
    }
}