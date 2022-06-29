package com.sd.lib.stream.binder

import android.app.Activity
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import com.sd.lib.stream.FStream
import java.lang.ref.WeakReference

/**
 * 将流对象和[Activity]绑定，在[Window.getDecorView]对象被移除的时候取消注册流对象
 */
internal class ActivityStreamBinder(
    stream: FStream,
    target: Activity,
) : StreamBinder<Activity>(stream, target) {

    private val _decorViewRef: WeakReference<View>

    override fun bind(): Boolean {
        val activity = target ?: return false
        if (activity.isFinishing) return false
        val decorView = _decorViewRef.get() ?: return false
        if (registerStream()) {
            decorView.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
            decorView.addOnAttachStateChangeListener(_onAttachStateChangeListener)
            return true
        }
        return false
    }

    private val _onAttachStateChangeListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
        }

        override fun onViewDetachedFromWindow(v: View) {
            destroy()
        }
    }

    override fun destroy() {
        super.destroy()
        _decorViewRef.get()?.removeOnAttachStateChangeListener(_onAttachStateChangeListener)
    }

    init {
        val window = requireNotNull(target.window) { "Bind stream failed because activity's window is null" }
        _decorViewRef = WeakReference(window.decorView)
    }
}