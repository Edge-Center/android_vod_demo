package ru.edgecenter.edge_vod.utils.connectivity_state

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Needed to check Internet access.
 * You can read more details [here](https://proandroiddev.com/connectivity-network-internet-state-change-on-android-10-and-above-311fb761925)
 * */
interface ConnectivityProvider {

    interface ConnectivityStateListener {
        fun onStateChange(state: NetworkState)
    }

    fun addListener(listener: ConnectivityStateListener)
    fun removeListener(listener: ConnectivityStateListener)

    fun getNetworkState(): NetworkState

    sealed class NetworkState {
        object NotConnectedState : NetworkState()

        sealed class ConnectedState(val hasInternet: Boolean) : NetworkState() {

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            data class Connected(val capabilities: NetworkCapabilities) : ConnectedState(
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            )
        }
    }

    companion object {

        fun createProvider(context: Context): ConnectivityProvider {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

            return ConnectivityProviderImpl(connectivityManager)
        }
    }
}