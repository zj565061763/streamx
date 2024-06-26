package com.sd.lib.stream

import java.util.concurrent.CopyOnWriteArrayList

/**
 * 流对象持有者，保存流接口映射的流对象列表
 */
internal class StreamHolder(clazz: Class<out FStream>) {
    /** 流接口 */
    private val _class: Class<out FStream> = clazz

    /** 流对象 */
    private val _streamHolder: MutableList<FStream> = CopyOnWriteArrayList()

    /** 设置了优先级的流对象  */
    private val _priorityStreamHolder: MutableSet<FStream> = mutableSetOf()

    /** 是否需要排序  */
    private var _isNeedSort = false

    /** 流对象数量 */
    val size: Int
        get() = _streamHolder.size

    /**
     * 添加流对象
     */
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
    fun remove(stream: FStream): Boolean {
        return _streamHolder.remove(stream).also {
            _priorityStreamHolder.remove(stream)
        }
    }

    /**
     * 返回流集合
     */
    fun toCollection(): Collection<FStream> {
        if (_isNeedSort) {
            _streamHolder.sortByDescending {
                FStreamManager.getConnection(it)?.getPriority(_class) ?: 0
            }
            _isNeedSort = false
            logMsg { "sort ${_class.name}" }
        }
        return _streamHolder
    }

    /**
     * 通知优先级变化
     */
    fun notifyPriorityChanged(stream: FStream, priority: Int) {
        if (!_streamHolder.contains(stream)) return

        if (priority == 0) {
            _priorityStreamHolder.remove(stream)
        } else {
            _priorityStreamHolder.add(stream)
        }
        _isNeedSort = _priorityStreamHolder.isNotEmpty()

        logMsg { "notifyPriorityChanged (${priority}) (${_class.name}) -> (${stream}) prioritySize:${_priorityStreamHolder.size}" }
    }
}