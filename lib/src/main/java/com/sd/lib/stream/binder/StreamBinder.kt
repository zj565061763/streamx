package com.sd.lib.stream.binder

import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import java.lang.ref.WeakReference

internal abstract class StreamBinder<T>(
    stream: FStream,
    target: T,
) {
    private val _streamRef = WeakReference(stream)
    private val _targetRef = WeakReference(target)

    /** 要绑定的目标 */
    val target: T? get() = _targetRef.get()

    /**
     * 绑定
     *
     * @return true-成功  false-失败
     */
    abstract fun bind(): Boolean

    /**
     * 注册流对象
     *
     * @return true-成功  false-失败
     */
    protected fun registerStream(): Boolean {
        val stream = _streamRef.get() ?: return false
        FStreamManager.register(stream)
        return true
    }

    /**
     * 取消注册流对象
     */
    protected fun unregisterStream() {
        val stream = _streamRef.get() ?: return
        FStreamManager.unregister(stream)
    }

    /**
     * 取消注册流对象，并解除绑定关系
     */
    open fun destroy() {
        unregisterStream()
        _streamRef.clear()
    }
}