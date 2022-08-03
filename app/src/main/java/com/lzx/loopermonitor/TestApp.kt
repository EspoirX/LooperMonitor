package com.lzx.loopermonitor

import android.app.Application
import com.lzx.library.LooperMonitor

class TestApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LooperMonitor.init(this)
    }
}