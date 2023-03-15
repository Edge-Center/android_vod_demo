package ru.edgecenter.edge_vod.screens.main.tabs.videos

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import edge_vod.R
import edge_vod.databinding.FragmentVodsBinding

class VODsFragment : Fragment(R.layout.fragment_vods) {

    private lateinit var binding: FragmentVodsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentVodsBinding.bind(view)

        val adapter = VODsPagerAdapter(requireActivity().supportFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.apply {
                text = listTabs[position]
                tab.view.setHorizontalGravity(Gravity.START)
                tab.view.setVerticalGravity(Gravity.CENTER_VERTICAL)
            }
        }.attach()
    }

    companion object {
        private val listTabs = listOf("Remote VODs", "Uploading VODs")
    }
}