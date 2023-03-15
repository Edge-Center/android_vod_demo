package ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.adapter

import androidx.recyclerview.widget.DiffUtil
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.UiVideoItem

class VideosDiffCallback(
    private val oldList: List<UiVideoItem>,
    private val newList: List<UiVideoItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldVideoItem = oldList[oldItemPosition]
        val newVideoItem = newList[newItemPosition]

        return oldVideoItem.id == newVideoItem.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldVideoItem = oldList[oldItemPosition]
        val newVideoItem = newList[newItemPosition]

        return oldVideoItem == newVideoItem
    }
}