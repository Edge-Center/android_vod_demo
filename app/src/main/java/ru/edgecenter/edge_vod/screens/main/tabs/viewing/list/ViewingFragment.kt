package ru.edgecenter.edge_vod.screens.main.tabs.viewing.list

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.navOptions
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.upstream.BandwidthMeter
import edge_vod.R
import edge_vod.databinding.FragmentViewingBinding
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.RemoteVODsViewModel
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.RemoteVODsState
import ru.edgecenter.edge_vod.utils.extensions.findTopNavController
import ru.edgecenter.edge_vod.utils.viewModelCreator

class ViewingFragment : Fragment(R.layout.fragment_viewing) {

    private lateinit var binding: FragmentViewingBinding
    private val viewModel by viewModelCreator {
        RemoteVODsViewModel(requireActivity().application)
    }

    private val viewingPagerAdapter by lazy { ViewingPagerAdapter(this) }

    private val _scrollDirection =
        MutableLiveData<ScrollDirection>(ScrollDirection.Forward(0))
    val scrollDirection: LiveData<ScrollDirection> = _scrollDirection

    private val _currentVolume = MutableLiveData<Float>()
    val currentVolume: LiveData<Float> = _currentVolume

    var exoPlayer1: ExoPlayer? = null
    var exoPlayer2: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bandwidthMeter =
            (requireActivity().application as ru.edgecenter.edge_vod.EdgeApp).exoPlayerUtils.bandwidthMeter

        exoPlayer1 = buildExoPlayer(bandwidthMeter)
        exoPlayer2 = buildExoPlayer(bandwidthMeter)

        exoPlayer1?.addListener(playerListener)
        exoPlayer2?.addListener(playerListener)
        _currentVolume.value = exoPlayer1!!.volume
    }

    private fun buildExoPlayer(bandwidthMeter: BandwidthMeter): ExoPlayer {
        return ExoPlayer.Builder(requireContext())
            .setBandwidthMeter(bandwidthMeter)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentViewingBinding.bind(view)

        with(binding.viewPager) {
            adapter = viewingPagerAdapter
            registerOnPageChangeCallback(pageChangeCallback)
            offscreenPageLimit = 3
        }
        binding.refresherVODs.apply {
            setColorSchemeResources(R.color.grey_100)
            setProgressBackgroundColorSchemeResource(R.color.grey_800)
            setOnRefreshListener {
                viewModel.getVideos()
            }
        }

        observeRemoteVODsState()
    }

    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    override fun onDestroy() {
        exoPlayer1?.removeListener(playerListener)
        exoPlayer1?.release()
        exoPlayer1 = null

        exoPlayer2?.removeListener(playerListener)
        exoPlayer2?.release()
        exoPlayer2 = null

        super.onDestroy()
    }

    private val playerListener =  object : Listener {
        override fun onVolumeChanged(volume: Float) {
            _currentVolume.value = volume
            super.onVolumeChanged(volume)
        }
    }

    private fun observeRemoteVODsState() {
        viewModel.remoteVideosState.observe(viewLifecycleOwner) {
            when(it) {
                is RemoteVODsState.Empty -> {
                    binding.apply {
                        refresherVODs.isRefreshing = false
                        noVideos.visibility = View.VISIBLE
                    }
                }
                is RemoteVODsState.Loading -> {
                    binding.apply {
                        refresherVODs.isRefreshing = true
                        noVideos.visibility = View.GONE
                    }
                }
                is RemoteVODsState.Success -> {
                    binding.apply {
                        refresherVODs.isRefreshing = false
                        noVideos.visibility = View.GONE
                    }
                    viewingPagerAdapter.setData(it.videos)
                }
                is RemoteVODsState.AccessDenied -> {
                    findTopNavController().navigate(
                        R.id.loginFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.main_graph) { inclusive = true }
                        }
                    )
                }
                is RemoteVODsState.Error -> {
                    binding.apply {
                        refresherVODs.isRefreshing = false
                        noVideos.visibility = View.VISIBLE
                    }

                    Toast.makeText(
                        requireContext(),
                        R.string.failed_load_videos,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            val offset = position - (_scrollDirection.value?.displayedPosition ?: 0)

            if (offset > 0) {
                _scrollDirection.value = ScrollDirection.Forward(position)
            } else if (offset < 0) {
                _scrollDirection.value = ScrollDirection.Back(position)
            }
        }
    }

    companion object {
        const val fragmentsVisibilityScope = 2
    }

}