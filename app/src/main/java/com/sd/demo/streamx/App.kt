package com.sd.demo.streamx

import android.app.Application
import com.sd.lib.stream.FStream

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 打开调试模式
        FStream.setDebug(true)
    }
}