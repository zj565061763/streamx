package com.sd.lib.stream.binder

import android.app.Activity
import android.view.View
import android.view.View.OnAttachStateChangeListener
import com.sd.lib.stream.FStream
import com.sd.lib.stream.fPreferActivityContext

/**
 * 将流对象和[View]绑定，监听[View.OnAttachStateChangeListener]自动注册和取消注册
 */
internal class ViewStreamBinder(
    stream: FStream,
    target: View,
) : StreamBinder<View>(stream, target) {

    override fun bindImpl(target: View): Boolean {
        val context = target.context.fPreferActivityContext()
        if (context is Activity && context.isFinishing) {
            return false
        }

        val listenerTask = Runnable {
            target.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
            target.addOnAttachStateChangeListener(_onAttachStateChangeListener)
        }

        return if (target.isAttachedToWindow) {
            registerStream().also { register ->
                if (register) listenerTask.run()
            }
        } else {
            listenerTask.run()
            true
        }
    }

    private val _onAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            registerStream()
        }

        override fun onViewDetachedFromWindow(v: View) {
            unregisterStream()
        }
    }

    override fun onDestroy(target: View) {
        target.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
    }
}