package com.sd.lib.stream.binder

import android.app.Activity
import android.view.View
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import java.util.WeakHashMap

internal object FStreamBinder {
    private val _mapStreamBinder: MutableMap<FStream, StreamBinder<*>> = WeakHashMap()

    /**
     * 把[stream]和[target]绑定，[target]销毁之后自动取消注册
     * [ActivityStreamBinder]
     *
     * @return true-绑定成功或者已绑定  false-绑定失败
     */
    fun bindActivity(stream: FStream, target: Activity): Boolean {
        return bindStreamInternal(stream, target) { ActivityStreamBinder(stream, target) }
    }

    /**
     * 把[stream]和[target]绑定，自动注册和取消注册
     * [ViewStreamBinder]
     *
     * @return true-绑定成功或者已绑定  false-绑定失败
     */
    fun bindView(stream: FStream, target: View): Boolean {
        return bindStreamInternal(stream, target) { ViewStreamBinder(stream, target) }
    }

    private fun <T> bindStreamInternal(stream: FStream, target: T, factory: () -> StreamBinder<T>): Boolean {
        synchronized(FStreamManager) {
            val oldBinder = _mapStreamBinder[stream]
            if (oldBinder != null) {
                if (oldBinder.getTarget() === target) {
                    // 已经绑定过了
                    return true
                } else {
                    // target发生变化，先取消绑定
                    unbindStream(stream)
                }
            }

            val binder = factory()
            return binder.bind().also { success ->
                if (success) {
                    _mapStreamBinder[stream] = binder
                }
            }
        }
    }

    /**
     * 解绑并取消注册
     *
     * @return true-解绑成功  false-未绑定过
     */
    fun unbindStream(stream: FStream): Boolean {
        synchronized(FStreamManager) {
            val binder = _mapStreamBinder.remove(stream) ?: return false
            binder.destroy()
            return true
        }
    }
}