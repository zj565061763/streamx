package com.sd.lib.stream.binder

import android.app.Activity
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import com.sd.lib.stream.FStream

/**
 * 将流对象和[Activity]绑定，在[Window.getDecorView]被移除的时候取消注册
 */
internal class ActivityStreamBinder(
    stream: FStream,
    target: Activity,
) : StreamBinder<Activity>(stream, target) {

    override fun bindImpl(target: Activity): Boolean {
        if (target.isFinishing) return false

        val decorView = requireNotNull(target.window) {
            "Bind stream failed because activity's window is null."
        }.decorView

        return registerStream().also { register ->
            if (register) {
                decorView.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
                decorView.addOnAttachStateChangeListener(_onAttachStateChangeListener)
            }
        }
    }

    private val _onAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
        }

        override fun onViewDetachedFromWindow(v: View) {
            destroy()
        }
    }

    override fun onDestroy(target: Activity) {
        target.window?.decorView?.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
    }
}