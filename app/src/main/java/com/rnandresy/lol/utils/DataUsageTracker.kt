package com.rnandresy.lol.utils

import android.net.TrafficStats
import android.os.Process

class DataUsageTracker {
    private val sessionStartRx: Long
    private val sessionStartTx: Long

    init {
        sessionStartRx = getSafeUidRxBytes()
        sessionStartTx = getSafeUidTxBytes()
    }

    private fun getSafeUidRxBytes(): Long {
        val v = TrafficStats.getUidRxBytes(Process.myUid())
        return if (v == TrafficStats.UNSUPPORTED.toLong()) 0L else v
    }

    private fun getSafeUidTxBytes(): Long {
        val v = TrafficStats.getUidTxBytes(Process.myUid())
        return if (v == TrafficStats.UNSUPPORTED.toLong()) 0L else v
    }

    fun getSessionBytes(): Long {
        val rx = (getSafeUidRxBytes() - sessionStartRx).coerceAtLeast(0)
        val tx = (getSafeUidTxBytes() - sessionStartTx).coerceAtLeast(0)
        return rx + tx
    }

    fun getSessionMB(): Float = getSessionBytes() / (1024f * 1024f)
}