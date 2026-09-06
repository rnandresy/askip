package com.rnandresy.lol.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class ConnectionType { NONE, WIFI, CELLULAR, OTHER }

data class NetworkStatus(
    val isOnline: Boolean = true,
    val type: ConnectionType = ConnectionType.OTHER,
    val isMetered: Boolean = false
) {
    /** Data mobile payante : on limite les médias lourds. */
    val shouldSaveData: Boolean get() = isMetered || type == ConnectionType.CELLULAR
}

class NetworkMonitor(private val context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    /** Flow de l'état réseau — se met à jour à chaque changement. */
    val status: Flow<NetworkStatus> = callbackFlow {

        fun current(): NetworkStatus {
            val net  = cm.activeNetwork ?: return NetworkStatus(false, ConnectionType.NONE)
            val caps = cm.getNetworkCapabilities(net)
                ?: return NetworkStatus(false, ConnectionType.NONE)

            val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> ConnectionType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                else -> ConnectionType.OTHER
            }

            val metered = !caps.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
            )

            return NetworkStatus(online, type, metered)
        }

        trySend(current())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network)              { trySend(current()) }
            override fun onLost(network: Network)                   { trySend(current()) }
            override fun onCapabilitiesChanged(
                network: Network, caps: NetworkCapabilities
            )                                                        { trySend(current()) }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()

    /** Vérification synchrone ponctuelle. */
    fun isOnlineNow(): Boolean {
        val net  = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}