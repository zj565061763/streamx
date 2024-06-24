package com.sd.lib.stream

/**
 * 流管理类
 */
internal object FStreamManager {
    private val _typedStreamHolder: MutableMap<Class<out FStream>, StreamHolder> = mutableMapOf()
    private val _streamConnections: MutableMap<FStream, StreamConnection> = mutableMapOf()

    var isDebug = false

    /**
     * 注册流对象
     */
    fun register(stream: FStream): StreamConnection {
        synchronized(FStreamManager) {
            val connection = _streamConnections[stream]
            if (connection != null) return connection

            val classes = findStreamInterface(stream.javaClass).also {
                if (it.isEmpty()) error("stream interface was not found in $stream")
            }

            for (clazz in classes) {
                val holder = _typedStreamHolder.getOrPut(clazz) { StreamHolder(clazz) }
                if (holder.add(stream)) {
                    logMsg { "+++++ (${clazz.name}) -> (${stream}) size:${holder.size}" }
                }
            }

            return StreamConnection(stream, classes).also {
                _streamConnections[stream] = it
            }
        }
    }

    /**
     * 取消注册流对象
     */
    fun unregister(stream: FStream) {
        synchronized(FStreamManager) {
            val connection = _streamConnections.remove(stream) ?: return
            val classes = connection.streamClasses
            for (clazz in classes) {
                val holder = _typedStreamHolder[clazz] ?: continue
                if (holder.remove(stream)) {
                    if (holder.size <= 0) {
                        _typedStreamHolder.remove(clazz)
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
        synchronized(FStreamManager) {
            return _streamConnections[stream]
        }
    }

    fun getStreams(clazz: Class<out FStream>): Collection<FStream>? {
        synchronized(FStreamManager) {
            return _typedStreamHolder[clazz]?.toCollection()
        }
    }

    fun notifyPriorityChanged(
        connection: StreamConnection,
        clazz: Class<out FStream>,
        priority: Int,
    ) {
        synchronized(FStreamManager) {
            if (connection.isConnected()) {
                val holder = _typedStreamHolder[clazz]
                holder?.notifyPriorityChanged(connection.stream, priority)
            }
        }
    }
}