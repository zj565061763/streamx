package com.sd.lib.stream

import android.app.Activity
import android.view.View
import com.sd.lib.stream.binder.ActivityStreamBinder
import com.sd.lib.stream.binder.StreamBinder
import com.sd.lib.stream.binder.ViewStreamBinder
import java.util.WeakHashMap

/**
 * 流管理类
 */
internal object FStreamManager {
    private val _mapStreamHolder: MutableMap<Class<out FStream>, StreamHolder> = hashMapOf()
    private val _mapStreamConnection: MutableMap<FStream, StreamConnection> = hashMapOf()

    var isDebug = false

    /**
     * 注册流对象
     */
    fun register(stream: FStream): StreamConnection {
        synchronized(this@FStreamManager) {
            val connection = _mapStreamConnection[stream]
            if (connection != null) return connection

            val classes = findStreamInterface(stream.javaClass).also {
                if (it.isEmpty()) error("stream interface was not found in $stream")
            }

            for (clazz in classes) {
                val holder = _mapStreamHolder[clazz] ?: StreamHolder(clazz).also {
                    _mapStreamHolder[clazz] = it
                }
                if (holder.add(stream)) {
                    logMsg { "+++++ (${clazz.name}) -> (${stream}) size:${holder.size}" }
                }
            }

            return StreamConnection(stream, classes).also {
                _mapStreamConnection[stream] = it
            }
        }
    }

    /**
     * 取消注册流对象
     */
    fun unregister(stream: FStream) {
        synchronized(this@FStreamManager) {
            val connection = _mapStreamConnection.remove(stream) ?: return
            val classes = connection.streamClasses
            for (clazz in classes) {
                val holder = _mapStreamHolder[clazz] ?: continue
                if (holder.remove(stream)) {
                    if (holder.size <= 0) {
                        _mapStreamHolder.remove(clazz)
                    }
                    logMsg { "----- (${clazz.name}) -> (${stream}) size:${holder.size}" }
                }
            }
        }
    }

    /**
     * 返回[stream]的连接对象
     */
    fun getConnection(stream: FStream): StreamConnection? {
        synchronized(this@FStreamManager) {
            return _mapStreamConnection[stream]
        }
    }

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

    private val _mapStreamBinder: MutableMap<FStream, StreamBinder<*>> = WeakHashMap()

    private fun <T> bindStreamInternal(stream: FStream, target: T, factory: () -> StreamBinder<T>): Boolean {
        synchronized(this@FStreamManager) {
            val oldBinder = _mapStreamBinder[stream]
            if (oldBinder != null) {
                if (oldBinder.target === target) {
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
        synchronized(this@FStreamManager) {
            val binder = _mapStreamBinder.remove(stream) ?: return false
            binder.destroy()
            return true
        }
    }

    fun getStreams(clazz: Class<out FStream>): Collection<FStream>? {
        synchronized(this@FStreamManager) {
            return _mapStreamHolder[clazz]?.toCollection()
        }
    }

    fun notifyPriorityChanged(
        priority: Int,
        stream: FStream,
        clazz: Class<out FStream>,
    ) {
        synchronized(this@FStreamManager) {
            _mapStreamHolder[clazz]?.notifyPriorityChanged(stream, priority)
        }
    }
}