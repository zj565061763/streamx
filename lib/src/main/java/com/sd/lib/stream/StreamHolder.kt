package com.sd.lib.stream

import android.util.Log

/**
 * 流对象持有者，保存流接口映射的流对象列表
 */
internal class StreamHolder(clazz: Class<out FStream>) {
    /** 流接口 */
    private val _class: Class<out FStream> = clazz

    /** 流对象 */
    private val _streamHolder: MutableCollection<FStream> = ArrayList()

    /** 设置了优先级的流对象  */
    private val _priorityStreamHolder: MutableMap<FStream, Int> = HashMap()

    /** 是否需要排序  */
    @Volatile
    private var _isNeedSort = false

    /** 流对象数量 */
    val size get() = _streamHolder.size

    /**
     * 添加流对象
     */
    @Synchronized
    fun add(stream: FStream): Boolean {
        return _streamHolder.add(stream).also { result ->
            if (result) {
                if (_priorityStreamHolder.isNotEmpty()) {
                    // 如果有流对象设置了优先级，则添加新流对象的时候标记为需要重新排序
                    _isNeedSort = true
                }
            }
        }
    }

    /**
     * 移除流对象
     */
    @Synchronized
    fun remove(stream: FStream): Boolean {
        return _streamHolder.remove(stream).also {
            _priorityStreamHolder.remove(stream)
        }
    }

    /**
     * 返回流集合
     */
    @Synchronized
    fun toCollection(): Collection<FStream> {
        sort()
        return _streamHolder
    }

    /**
     * 排序
     */
    private fun sort() {
        if (!_isNeedSort) return
        if (_streamHolder.size <= 1) return

        // 转数组排序
        val array = _streamHolder.toTypedArray().also {
            it.sortWith(StreamPriorityComparatorDesc())
        }

        // 把排序后的数组保存到容器
        _streamHolder.clear()
        _streamHolder.addAll(array)
        _isNeedSort = false

        if (FStreamManager.isDebug) {
            Log.i(FStream::class.java.simpleName, "sort stream for class:${_class.name}")
        }
    }

    /**
     * 通知优先级变化
     */
    @Synchronized
    fun notifyPriorityChanged(priority: Int, stream: FStream, clazz: Class<out FStream>) {
        if (!_streamHolder.contains(stream)) return

        if (priority == 0) {
            _priorityStreamHolder.remove(stream)
        } else {
            _priorityStreamHolder[stream] = priority
        }

        _isNeedSort = _priorityStreamHolder.isNotEmpty()

        if (FStreamManager.isDebug) {
            Log.i(
                FStream::class.java.simpleName,
                "notifyPriorityChanged priority:${priority} clazz:${clazz.name} priorityStreamHolder size:${_priorityStreamHolder.size} stream:${stream}"
            )
        }
    }

    private inner class StreamPriorityComparatorDesc : Comparator<FStream> {
        override fun compare(o1: FStream, o2: FStream): Int {
            val o1Connection = FStreamManager.getConnection(o1)
            val o2Connection = FStreamManager.getConnection(o2)

            return if (o1Connection != null && o2Connection != null) {
                o2Connection.getPriority(_class) - o1Connection.getPriority(_class)
            } else {
                0
            }
        }
    }
}