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
        return _mapItem[clazz]?.priority ?: 0
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
            checkStreamClass(clazz)
            checkClassAssignable(clazz)
            _mapItem[clazz]?.setPriority(priority)
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
        val map = HashMap<Class<out FStream>, ConnectionItem>()
        for (item in classes) {
            checkStreamClass(item)
            map[item] = object : ConnectionItem(item) {
                override fun onPriorityChanged(priority: Int, clazz: Class<out FStream>) {
                    val holder = FStreamManager.getStreamHolder(clazz)
                    holder?.notifyPriorityChanged(priority, stream, clazz)
                }
            }
        }
        _mapItem = Collections.unmodifiableMap(map)
    }
}

internal abstract class ConnectionItem {
    private val _class: Class<out FStream>

    /** 优先级  */
    @Volatile
    var priority = 0
        private set

    /** 是否停止分发  */
    @Volatile
    var shouldBreakDispatch = false
        private set

    constructor(clazz: Class<out FStream>) {
        _class = clazz
    }

    /**
     * 设置优先级
     */
    fun setPriority(priority: Int) {
        val isChanged: Boolean = synchronized(this@ConnectionItem) {
            if (this.priority != priority) {
                this.priority = priority
                true
            } else {
                false
            }
        }

        if (isChanged) {
            onPriorityChanged(priority, _class)
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
        shouldBreakDispatch = false
    }

    /**
     * 优先级变化回调
     */
    protected abstract fun onPriorityChanged(priority: Int, clazz: Class<out FStream>)
}