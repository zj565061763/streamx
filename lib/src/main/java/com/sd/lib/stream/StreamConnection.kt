package com.sd.lib.stream

import java.util.*

class StreamConnection internal constructor(
    stream: FStream,
    classes: Collection<Class<out FStream>>,
) {
    private val _stream = stream
    private val _mapItem: Map<Class<out FStream>, ConnectionItem>

    /**
     * 当前连接的所有流接口
     */
    internal val streamClasses: Collection<Class<out FStream>>
        get() = _mapItem.keys

    /**
     * 返回优先级
     */
    @JvmOverloads
    fun getPriority(clazz: Class<out FStream>? = null): Int {
        if (clazz != null) return _mapItem[clazz]!!.priority
        check(_mapItem.size == 1)
        return _mapItem[_mapItem.keys.first()]!!.priority
    }

    /**
     * 设置优先级
     */
    @JvmOverloads
    fun setPriority(priority: Int, clazz: Class<out FStream>? = null) {
        if (clazz == null) {
            for (item in _mapItem.values) {
                item.setPriority(priority)
            }
        } else {
            _mapItem[clazz]!!.setPriority(priority)
        }
    }

    /**
     * 停止分发
     */
    @JvmOverloads
    fun breakDispatch(clazz: Class<out FStream>? = null) {
        if (clazz != null) {
            _mapItem[clazz]!!.breakDispatch()
        } else {
            check(_mapItem.size == 1)
            _mapItem.values.first().breakDispatch()
        }
    }

    internal fun getItem(clazz: Class<out FStream>): ConnectionItem {
        return _mapItem[clazz]!!
    }

    init {
        val mapItem = classes.associateWith { item ->
            object : ConnectionItem() {
                override fun onPriorityChanged(priority: Int) {
                    FStreamManager.getStreamHolder(item)?.notifyPriorityChanged(priority, _stream, item)
                }
            }
        }
        _mapItem = Collections.unmodifiableMap(mapItem)
    }
}

internal abstract class ConnectionItem {
    /** 优先级  */
    @Volatile
    var priority = 0
        private set

    /** 是否停止分发  */
    @Volatile
    var shouldBreakDispatch = false
        private set

    /**
     * 设置优先级
     */
    fun setPriority(priority: Int) {
        synchronized(this@ConnectionItem) {
            if (this.priority != priority) {
                this.priority = priority
                true
            } else {
                false
            }
        }.let { changed ->
            if (changed) {
                onPriorityChanged(priority)
            }
        }
    }

    /**
     * 设置停止分发
     */
    fun breakDispatch() {
        shouldBreakDispatch = true
    }

    /**
     * 重置停止分发标志
     */
    fun resetBreakDispatch() {
        shouldBreakDispatch = false
    }

    /**
     * 优先级变化回调
     */
    protected abstract fun onPriorityChanged(priority: Int)
}