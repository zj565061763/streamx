package com.sd.lib.stream

import android.app.Activity
import android.view.View
import com.sd.lib.stream.binder.ActivityStreamBinder
import com.sd.lib.stream.binder.StreamBinder
import com.sd.lib.stream.binder.ViewStreamBinder
import com.sd.lib.stream.utils.findStreamClass
import com.sd.lib.stream.utils.logMsg
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 流管理类
 */
object FStreamManager {
    @Deprecated("")
    @JvmStatic
    val instance by lazy { FStreamManager }

    private val _mapStreamHolder: MutableMap<Class<out FStream>, StreamHolder> = ConcurrentHashMap()
    private val _mapStreamConnection: MutableMap<FStream, StreamConnection> = ConcurrentHashMap()
    private val _mapStreamBinder: MutableMap<FStream, StreamBinder<*>> = WeakHashMap()

    @JvmStatic
    var isDebug = false

    /**
     * 返回[stream]的连接对象
     */
    fun getConnection(stream: FStream): StreamConnection? {
        return _mapStreamConnection[stream]
    }

    /**
     * 注册流对象
     */
    @Synchronized
    fun register(stream: FStream): StreamConnection {
        val connection = _mapStreamConnection[stream]
        if (connection != null) return connection

        val classes = findStreamClass(stream.javaClass)
        for (clazz in classes) {
            var holder = _mapStreamHolder[clazz]
            if (holder == null) {
                holder = StreamHolder(clazz).also {
                    _mapStreamHolder[clazz] = it
                }
            }

            if (holder.add(stream)) {
                logMsg { "+++++ (${clazz.name}) -> (${stream}) size:${holder.size}" }
            }
        }

        return StreamConnection(stream, classes).also {
            _mapStreamConnection[stream] = it
        }
    }

    /**
     * 取消注册流对象
     */
    @Synchronized
    fun unregister(stream: FStream) {
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

    @Synchronized
    private fun <T> bindStreamInternal(stream: FStream, target: T, factory: () -> StreamBinder<T>): Boolean {
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

    /**
     * 解绑并取消注册
     *
     * @return true-解绑成功  false-未绑定过
     */
    @Synchronized
    fun unbindStream(stream: FStream): Boolean {
        val binder = _mapStreamBinder.remove(stream) ?: return false
        binder.destroy()
        return true
    }

    internal fun getStreamHolder(clazz: Class<out FStream>): StreamHolder? {
        return _mapStreamHolder[clazz]
    }
}