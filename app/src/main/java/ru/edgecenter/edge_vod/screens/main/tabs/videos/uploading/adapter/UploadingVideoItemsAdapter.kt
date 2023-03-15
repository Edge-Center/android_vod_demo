package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import edge_vod.R
import edge_vod.databinding.LiUploadingVodBinding
import ru.edgecenter.edge_vod.data.remote.UploadVideoWorker
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UiUploadingVideo

class UploadingVideoItemsAdapter :
    RecyclerView.Adapter<UploadingVideoItemsAdapter.UploadingVideoItemViewHolder>() {

    private val videoItems: MutableList<UiUploadingVideo> = ArrayList()
    private var eventListener: UploadVideoEvents? = null

    fun setData(newVideos: List<UiUploadingVideo>) {
        val diffCallback = UploadingVideosDiffCallback(videoItems, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videoItems.clear()
        videoItems.addAll(newVideos)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setEventListener(listener: UploadVideoEvents) {
        eventListener = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UploadingVideoItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiUploadingVodBinding.inflate(inflater, parent, false)

        return UploadingVideoItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UploadingVideoItemViewHolder, position: Int) {
        val videoItem = videoItems[position]
        holder.bind(videoItem, eventListener)
    }

    override fun getItemCount(): Int {
        return videoItems.size
    }

    class UploadingVideoItemViewHolder(
        private val binding: LiUploadingVodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(videoItem: UiUploadingVideo, eventListener: UploadVideoEvents?) {
            binding.apply {
                videoId.text = itemView.context.getString(
                    R.string.id,
                    videoItem.remoteData.uploadVideo.id
                )
                videoNameTV.text = videoItem.videoName

                watchVideo.setOnClickListener { eventListener?.onWatchVideo(videoItem) }
                toggleUpload.setOnClickListener {
                    videoItem.state?.value?.let {
                        if (it.state == WorkInfo.State.RUNNING) {
                            eventListener?.onCancelUpload(videoItem)
                        }
                        if (it.state == WorkInfo.State.CANCELLED || it.state == WorkInfo.State.FAILED) {
                            binding.toggleUpload.setImageResource(R.drawable.ic_cancel_24)
                            eventListener?.onResumeUpload(videoItem)
                        }
                    }
                }
            }
            videoItem.state?.observeForever(object : Observer<WorkInfo> {
                @SuppressLint("SetTextI18n")
                override fun onChanged(workInfo: WorkInfo) {
                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progressPercent = workInfo.progress.getInt(
                                UploadVideoWorker.UPLOADED_PERCENT,
                                0
                            )
                            binding.apply {
                                progressBar.progress = progressPercent
                                percentProgress.text = "$progressPercent%"
                            }
                        }
                        WorkInfo.State.CANCELLED, WorkInfo.State.FAILED -> {
                            binding.toggleUpload.setImageResource(R.drawable.ic_round_refresh_24)
                            videoItem.state?.removeObserver(this)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            binding.apply {
                                toggleUpload.isEnabled = false
                                toggleUpload.visibility = View.INVISIBLE
                                successUpload.visibility = View.VISIBLE

                                progressBar.progress = progressBar.max
                                percentProgress.text =
                                    itemView.context.getString(R.string.one_hundred_percent)
                            }
                            videoItem.state?.removeObserver(this)
                        }
                        else -> {}
                    }
                }
            })
        }
    }
}