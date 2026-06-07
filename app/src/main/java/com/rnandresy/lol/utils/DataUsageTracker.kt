package com.rnandresy.lol.utils

import android.net.TrafficStats
import android.os.Process

class DataUsageTracker {
    private val uid     = Process.myUid()
    private val startRx = safeBytes { TrafficStats.getUidRxBytes(uid) }
    private val startTx = safeBytes { TrafficStats.getUidTxBytes(uid) }

    private fun safeBytes(block: () -> Long): Long = runCatching {
        block().takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L
    }.getOrElse { 0L }

    fun getSessionBytes(): Long {
        val rx = (safeBytes { TrafficStats.getUidRxBytes(uid) } - startRx).coerceAtLeast(0L)
        val tx = (safeBytes { TrafficStats.getUidTxBytes(uid) } - startTx).coerceAtLeast(0L)
        return rx + tx
    }

    fun getSessionMB(): Float = getSessionBytes() / (1024f * 1024f)
}