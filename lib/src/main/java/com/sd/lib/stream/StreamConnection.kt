package com.sd.lib.stream

import java.util.Collections

class StreamConnection internal constructor(
    internal val stream: FStream,
    classes: Collection<Class<out FStream>>,
) {
    private val _mapItem: Map<Class<out FStream>, ConnectionItem>

    /**
     * 当前连接的所有流接口
     */
    internal val streamClasses: Collection<Class<out FStream>>
        get() = _mapItem.keys

    /**
     * 当前连接是否可用
     */
    val isConnected: Boolean
        get() = FStreamManager.getConnection(stream) === this

    /**
     * 返回优先级
     */
    @JvmOverloads
    fun getPriority(clazz: Class<out FStream>? = null): Int {
        return if (clazz != null) {
            getItem(clazz).priority
        } else {
            getSingleItem().priority
        }
    }

    /**
     * 设置优先级
     */
    @JvmOverloads
    fun setPriority(priority: Int, clazz: Class<out FStream>? = null) {
        if (clazz != null) {
            getItem(clazz).setPriority(priority)
        } else {
            getSingleItem().setPriority(priority)
        }
    }

    /**
     * 停止分发
     */
    @JvmOverloads
    fun breakDispatch(clazz: Class<out FStream>? = null) {
        if (clazz != null) {
            getItem(clazz).breakDispatch()
        } else {
            getSingleItem().breakDispatch()
        }
    }

    internal fun getItem(clazz: Class<out FStream>): ConnectionItem {
        return checkNotNull(_mapItem[clazz])
    }

    private fun getSingleItem(): ConnectionItem {
        check(_mapItem.size == 1) { "You should specified target class" }
        return _mapItem.values.first()
    }

    init {
        val mapItem = classes.associateWith { item ->
            object : ConnectionItem() {
                override fun onPriorityChanged(priority: Int) {
                    FStreamManager.notifyPriorityChanged(
                        connection = this@StreamConnection,
                        clazz = item,
                        priority = priority,
                    )
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
        synchronized(this@ConnectionItem) {
            shouldBreakDispatch = true
        }
    }

    /**
     * 重置停止分发标志
     */
    fun resetBreakDispatch() {
        synchronized(this@ConnectionItem) {
            shouldBreakDispatch = false
        }
    }

    /**
     * 优先级变化回调
     */
    protected abstract fun onPriorityChanged(priority: Int)
}