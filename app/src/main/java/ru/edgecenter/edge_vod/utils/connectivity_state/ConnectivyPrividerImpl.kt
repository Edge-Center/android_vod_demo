package ru.edgecenter.edge_vod.utils.connectivity_state

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper

class ConnectivityProviderImpl(
    private val connectivityManager: ConnectivityManager
) : ConnectivityProvider {

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<ConnectivityProvider.ConnectivityStateListener>()
    private var subscribed = false

    private val networkCallback = ConnectivityCallback()

    override fun addListener(listener: ConnectivityProvider.ConnectivityStateListener) {
        listeners.add(listener)
        listener.onStateChange(getNetworkState())
        verifySubscription()
    }

    override fun removeListener(listener: ConnectivityProvider.ConnectivityStateListener) {
        listeners.remove(listener)
        verifySubscription()
    }

    override fun getNetworkState(): ConnectivityProvider.NetworkState {
        val networkCaps =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        return if (networkCaps != null) {
            ConnectivityProvider.NetworkState.ConnectedState.Connected(networkCaps)
        } else {
            ConnectivityProvider.NetworkState.NotConnectedState
        }
    }

    private fun verifySubscription() {
        if (!subscribed && listeners.isNotEmpty()) {
            subscribe()
            subscribed = true
        } else if (subscribed && listeners.isEmpty()) {
            unsubscribe()
            subscribed = false
        }
    }

    private fun dispatchChange(state: ConnectivityProvider.NetworkState) {
        handler.post {
            for (listener in listeners) {
                listener.onStateChange(state)
            }
        }
    }

    private fun subscribe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }
    }

    private fun unsubscribe() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private inner class ConnectivityCallback : ConnectivityManager.NetworkCallback() {

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            dispatchChange(
                ConnectivityProvider.NetworkState.ConnectedState.Connected(
                    networkCapabilities
                )
            )
        }

        override fun onLost(network: Network) {
            dispatchChange(ConnectivityProvider.NetworkState.NotConnectedState)
        }
    }
}