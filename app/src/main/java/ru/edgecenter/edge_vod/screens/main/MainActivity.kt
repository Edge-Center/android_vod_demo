package ru.edgecenter.edge_vod.screens.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import edge_vod.R

class MainActivity : AppCompatActivity() {

    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = getRootNavController()
        val isAuth = intent.getBooleanExtra(isAuthKey, false)

        setStartDestination(isAuth, navController!!)
    }

    override fun onDestroy() {
        navController = null
        super.onDestroy()
    }

    private fun getRootNavController(): NavController {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        return navHost.navController
    }

    private fun setStartDestination(isAuth: Boolean, navController: NavController) {
        val graph = navController.navInflater.inflate(R.navigation.main_graph)
        graph.setStartDestination(
            if (isAuth) {
                R.id.tabsFragment
            } else {
                R.id.loginFragment
            }
        )
        navController.graph = graph
    }

    companion object {
        const val isAuthKey = "isAuthKey"
    }
}