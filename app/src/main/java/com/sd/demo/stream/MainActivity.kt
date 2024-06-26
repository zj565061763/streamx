package com.sd.demo.stream

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sd.lib.stream.FStream
import com.sd.lib.stream.bindActivity
import com.sd.lib.stream.registerStream
import com.sd.lib.stream.unregisterStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 绑定流对象，绑定之后会自动取消注册
        _callback1.bindActivity(this)

        // 注册流对象，并设置优先级，数值越大越先被通知，默认优先级：0
        _callback2.registerStream().setPriority(-1)

        // 绑定流对象，绑定之后会自动取消注册
        _callback3.bindActivity(this)
    }

    private val _callback1 = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            return "1"
        }
    }

    private val _callback2 = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            return "2"
        }
    }

    private val _callback3 = object : TestFragment.FragmentCallback {
        override fun getDisplayContent(): String {
            return "3"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * 手动注册和取消注册
         */
        _callback2.unregisterStream()
    }

    companion object {
        init {
            // 打开调试模式
            FStream.setDebug(true)
        }
    }
}