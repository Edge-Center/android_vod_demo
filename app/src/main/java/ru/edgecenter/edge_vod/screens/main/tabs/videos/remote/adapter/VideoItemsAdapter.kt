package ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import edge_vod.R
import edge_vod.databinding.LiVodBinding
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.UiVideoItem

typealias OnVideoItemClickListener = (UiVideoItem) -> Unit

class VideoItemsAdapter :
    RecyclerView.Adapter<VideoItemsAdapter.VideoItemViewHolder>() {

    private val videoItems: MutableList<UiVideoItem> = ArrayList()
    private var onItemClickListener: OnVideoItemClickListener? = null

    fun setData(newVideos: List<UiVideoItem>) {
        val diffCallback = VideosDiffCallback(videoItems, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videoItems.clear()
        videoItems.addAll(newVideos)
        diffResult.dispatchUpdatesTo(this)
    }

    fun setOnItemClickListener(listener: OnVideoItemClickListener) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiVodBinding.inflate(inflater, parent, false)

        return VideoItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoItemViewHolder, position: Int) {
        val videoItem = videoItems[position]

        holder.bind(videoItem)
        holder.setOnClickListener(videoItem, onItemClickListener)
    }

    override fun getItemCount(): Int {
        return videoItems.size
    }

    class VideoItemViewHolder(
        private val binding: LiVodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(videoItem: UiVideoItem) {

            Glide.with(binding.root)
                .load(videoItem.previewUri)
                .into(binding.videoPreview)

            binding.videoId.text = itemView.context.getString(R.string.id, videoItem.id)
            binding.videoNameTV.text = videoItem.name
        }

        fun setOnClickListener(videoItem: UiVideoItem, onClick: OnVideoItemClickListener?) {
            binding.root.setOnClickListener {
                onClick?.invoke(videoItem)
            }
        }
    }
}