package com.sd.lib.demo.streamx

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import com.sd.lib.stream.registerStream
import com.sd.lib.stream.unregisterStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 注册流对象
        _callback1.registerStream()

        // 注册流对象，并设置优先级，数值越大越先被通知，默认优先级：0
        _callback2.registerStream().setPriority(-1)

        // 绑定流对象，绑定之后会自动取消注册
        FStreamManager.bindActivity(_callback3, this)
    }

    private val _callback1 = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            return "1"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    private val _callback2 = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            return "2"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    private val _callback3 = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            return "3"
        }

        override fun getTagForStream(clazz: Class<out FStream?>): Any? {
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * 手动取消注册
         */
        _callback1.unregisterStream()
        _callback2.unregisterStream()
    }
}