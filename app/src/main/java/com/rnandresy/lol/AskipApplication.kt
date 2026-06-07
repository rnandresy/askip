package com.rnandresy.lol

import android.app.Application
import com.rnandresy.lol.utils.NotificationHelper

class AskipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper(this).createChannels()
    }
}