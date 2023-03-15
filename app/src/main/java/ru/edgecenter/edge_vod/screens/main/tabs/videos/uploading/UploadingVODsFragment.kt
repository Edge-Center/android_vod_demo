package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import edge_vod.R
import edge_vod.databinding.FragmentUploadingVodsBinding
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.adapter.UploadVideoEvents
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.adapter.UploadingVideoItemsAdapter
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UiUploadingVideo
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UploadingState
import ru.edgecenter.edge_vod.screens.main.video_player.VideoPlayerFragment
import ru.edgecenter.edge_vod.utils.extensions.findTopNavController
import ru.edgecenter.edge_vod.utils.sharedViewModelCreator

class UploadingVODsFragment : Fragment(R.layout.fragment_uploading_vods) {

    private var binding: FragmentUploadingVodsBinding? = null
    private val sharedViewModel: UploadingVODsViewModel by sharedViewModelCreator {
        UploadingVODsViewModel(requireActivity().application)
    }

    private val videoItemsAdapter = UploadingVideoItemsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUploadingVodsBinding.bind(view)

        initUI(binding!!)
        videoItemsAdapter.setEventListener(eventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initUI(binding: FragmentUploadingVodsBinding) {
        binding.uploadingVODsRV.apply {
            adapter = videoItemsAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
        }
        observeUploadingVideoState(binding)
    }

    private fun observeUploadingVideoState(binding: FragmentUploadingVodsBinding) {
        sharedViewModel.uploadingVideos.observe(viewLifecycleOwner) {
            videoItemsAdapter.setData(it)
            binding.noVideos.visibility = if (it.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        sharedViewModel.uploadingVideoState.observe(viewLifecycleOwner) { state: UploadingState? ->
            state?.let {
                val message = when (it) {
                    is UploadingState.Success -> {
                        getString(R.string.video_sent_success, it.videoName)
                    }
                    is UploadingState.Canceled -> {
                        getString(R.string.cancel_upload_video, it.videoName)
                    }
                    is UploadingState.Failure -> {
                        getString(R.string.video_sent_failed, it.videoName)
                    }
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private val eventListener = object : UploadVideoEvents {
        override fun onWatchVideo(video: UiUploadingVideo) {
            findTopNavController().navigate(
                R.id.action_tabsFragment_to_videoPlayerFragment,
                bundleOf(
                    VideoPlayerFragment.VIDEO_TITLE_KEY to video.videoName,
                    VideoPlayerFragment.VIDEO_URI_KEY to video.localUri
                )
            )
        }

        override fun onCancelUpload(video: UiUploadingVideo) {
            sharedViewModel.cancelUpload(video.remoteData.uploadVideo.id)
        }

        override fun onResumeUpload(video: UiUploadingVideo) {
            sharedViewModel.resumeUpload(video)
        }
    }
}