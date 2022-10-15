package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam
import com.sd.lib.stream.logMsg
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * 用弱引用缓存流对象的工厂
 */
class WeakCacheStreamFactory : CacheableStreamFactory() {
    private val _refQueue = ReferenceQueue<FStream>()
    private val _streamHolder: MutableMap<Class<out FStream>, StreamRef<FStream>> = HashMap()

    override fun getCache(param: CreateParam): FStream? {
        return _streamHolder[param.classStream]?.get()
    }

    override fun setCache(param: CreateParam, stream: FStream) {
        releaseReference()

        val ref = StreamRef(param.classStream, stream, _refQueue)
        _streamHolder[param.classStream] = ref

        logMsg(WeakCacheStreamFactory::class.java.simpleName) {
            "+++++ class:${param.classStream.name} stream:${stream} size:${_streamHolder.size}"
        }
    }

    private fun releaseReference() {
        var count = 0
        while (true) {
            val reference = _refQueue.poll()
            if (reference is StreamRef) {
                _streamHolder.remove(reference.clazz)
                count++
            } else {
                break
            }
        }

        if (count > 0) {
            logMsg(WeakCacheStreamFactory::class.java.simpleName) {
                "releaseReference count:${count} size:${_streamHolder.size}"
            }
        }
    }
}

private class StreamRef<T>(
    val clazz: Class<*>,
    referent: T,
    q: ReferenceQueue<in T>,
) : WeakReference<T>(referent, q)