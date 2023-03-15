package ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.adapter

import androidx.recyclerview.widget.DiffUtil
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.model.UiUploadingVideo

class UploadingVideosDiffCallback(
    private val oldList: List<UiUploadingVideo>,
    private val newList: List<UiUploadingVideo>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldVideoItem = oldList[oldItemPosition]
        val newVideoItem = newList[newItemPosition]

        return oldVideoItem.remoteData.uploadVideo.id == newVideoItem.remoteData.uploadVideo.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldVideoItem = oldList[oldItemPosition]
        val newVideoItem = newList[newItemPosition]

        return oldVideoItem.state === newVideoItem.state
    }
}