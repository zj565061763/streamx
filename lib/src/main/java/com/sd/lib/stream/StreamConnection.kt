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
    fun getPriority(clazz: Class<out FStream>): Int {
        return _mapItem[clazz]!!.priority
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
    fun breakDispatch(clazz: Class<out FStream>) {
        checkStreamClass(clazz)
        checkClassAssignable(clazz)
        _mapItem[clazz]?.breakDispatch()
    }

    internal fun getItem(clazz: Class<out FStream>): ConnectionItem? {
        checkStreamClass(clazz)
        return _mapItem[clazz]
    }

    private fun checkClassAssignable(clazz: Class<out FStream>) {
        require(clazz.isAssignableFrom(_stream.javaClass)) { "class is not assignable from ${_stream.javaClass.name} class:${clazz.name}" }
    }

    private fun checkStreamClass(clazz: Class<out FStream>) {
        require(clazz.isInterface) { "class must be an interface class:${clazz.name}" }
    }

    init {
        val mapItem = classes.associateWith { item ->
            object : ConnectionItem() {
                override fun onPriorityChanged(priority: Int) {
                    FStreamManager.getStreamHolder(item)?.notifyPriorityChanged(priority, stream, item)
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