package ru.edgecenter.edge_vod.screens.main.tabs.viewing.list

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.adapter.VideosDiffCallback
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.model.UiVideoItem
import ru.edgecenter.edge_vod.screens.main.tabs.viewing.list.item.VodFragment

class ViewingPagerAdapter(parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {

    private val videoItems: MutableList<UiVideoItem> = ArrayList()

    fun setData(newVideoItems: List<UiVideoItem>) {
        val diffCallback = VideosDiffCallback(videoItems, newVideoItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videoItems.clear()
        videoItems.addAll(newVideoItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return videoItems.size
    }

    override fun createFragment(position: Int): Fragment {
        return VodFragment.newInstance(videoItem = videoItems[position], position)
    }
}