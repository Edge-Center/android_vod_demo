package ru.edgecenter.edge_vod.screens.main.tabs

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import edge_vod.R
import edge_vod.databinding.FragmentTabsBinding

class TabsFragment : Fragment(R.layout.fragment_tabs) {

    private lateinit var binding: FragmentTabsBinding
    private var navController: NavController? = null

    private var isCurrentlyBottomMenuDark = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTabsBinding.bind(view)

        val navHost = childFragmentManager.findFragmentById(R.id.tabsContainer) as NavHostFragment
        navController = navHost.navController

        NavigationUI.setupWithNavController(binding.bottomMenu, navController!!)
        navController?.addOnDestinationChangedListener(destinationListener)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val previousDestination = navController?.previousBackStackEntry?.destination

            if (previousDestination != null) {
                navController?.popBackStack(previousDestination.id, false)
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        navController?.removeOnDestinationChangedListener(destinationListener)
        super.onDestroyView()
    }

    private val destinationListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            val needDarkBottomMenu = destination.id == R.id.viewingFragment ||
                    destination.id == R.id.uploadVideoFragment

            if (needDarkBottomMenu && !isCurrentlyBottomMenuDark) {
                binding.bottomMenu.setBackgroundColor(requireContext().getColor(R.color.dark_grey))
                isCurrentlyBottomMenuDark = true
            }

            if (!needDarkBottomMenu && isCurrentlyBottomMenuDark) {
                binding.bottomMenu.setBackgroundColor(requireContext().getColor(R.color.white))
                isCurrentlyBottomMenuDark = false
            }
        }

}