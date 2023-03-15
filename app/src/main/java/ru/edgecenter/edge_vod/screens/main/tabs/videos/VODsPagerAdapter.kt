package ru.edgecenter.edge_vod.screens.main.tabs.videos

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.edgecenter.edge_vod.screens.main.tabs.videos.remote.RemoteVODsFragment
import ru.edgecenter.edge_vod.screens.main.tabs.videos.uploading.UploadingVODsFragment

class VODsPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = COUNT_TABS

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            RemoteVODsFragment()
        } else {
            UploadingVODsFragment()
        }
    }

    companion object {
        const val COUNT_TABS = 2
    }
}