package com.sd.lib.stream

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * 默认流管理
 *
 * 如果代理对象方法触发的时候未找到注册的流对象，则会查找是否存在默认流class，
 * 如果存在默认流class，则用默认流class创建默认流对象来触发，默认流必须包含空构造方法
 */
object DefaultStreamManager {
    /** 映射流接口和默认流class */
    private val _mapDefaultStreamClass: MutableMap<Class<out FStream>, Class<out FStream>> = HashMap()

    /** 默认流对象工厂 */
    private var _streamFactory = WeakCacheStreamFactory()

    /**
     * 注册默认流class
     */
    @JvmStatic
    fun register(defaultClass: Class<out FStream>) {
        synchronized(this@DefaultStreamManager) {
            val classes = findStreamClass(defaultClass)
            for (item in classes) {
                _mapDefaultStreamClass[item] = defaultClass
            }
        }
    }

    /**
     * 取消注册默认流class
     */
    @JvmStatic
    fun unregister(defaultClass: Class<out FStream>) {
        synchronized(this@DefaultStreamManager) {
            val classes = findStreamClass(defaultClass)
            for (item in classes) {
                _mapDefaultStreamClass.remove(item)
            }
        }
    }

    /**
     * 返回默认流对象
     */
    internal fun getStream(clazz: Class<out FStream>): FStream? {
        synchronized(this@DefaultStreamManager) {
            val defaultClass = _mapDefaultStreamClass[clazz] ?: return null
            return _streamFactory.create(DefaultStreamFactory.CreateParam(clazz, defaultClass))
        }
    }
}

// ---------- factory ----------

/**
 * 默认流对象工厂
 */
private interface DefaultStreamFactory {
    /**
     * 创建流对象
     */
    fun create(param: CreateParam): FStream

    class CreateParam(
        /** 流接口 */
        val classStream: Class<out FStream>,
        /** 流接口实现类 */
        val classStreamDefault: Class<out FStream>,
    )
}


private abstract class CacheableStreamFactory : DefaultStreamFactory {

    final override fun create(param: DefaultStreamFactory.CreateParam): FStream {
        val cache = getCache(param)
        if (cache != null) return cache
        return createStream(param).also {
            setCache(param, it)
        }
    }

    /**
     * 创建Stream对象
     */
    protected open fun createStream(param: DefaultStreamFactory.CreateParam): FStream {
        return param.classStreamDefault.newInstance()
    }

    /**
     * 获取缓存
     */
    protected abstract fun getCache(param: DefaultStreamFactory.CreateParam): FStream?

    /**
     * 设置缓存
     */
    protected abstract fun setCache(param: DefaultStreamFactory.CreateParam, stream: FStream)
}


/**
 * 用弱引用缓存流对象的工厂
 */
private class WeakCacheStreamFactory : CacheableStreamFactory() {
    private val _refQueue = ReferenceQueue<FStream>()
    private val _streamHolder: MutableMap<Class<out FStream>, StreamRef<FStream>> = HashMap()

    override fun getCache(param: DefaultStreamFactory.CreateParam): FStream? {
        return _streamHolder[param.classStream]?.get()
    }

    override fun setCache(param: DefaultStreamFactory.CreateParam, stream: FStream) {
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
            val reference = _refQueue.poll() ?: break
            if (reference is StreamRef) {
                _streamHolder.remove(reference.clazz)
                count++
            } else {
                error("Unknown ref $reference")
            }
        }

        if (count > 0) {
            logMsg(WeakCacheStreamFactory::class.java.simpleName) {
                "releaseReference count:${count} size:${_streamHolder.size}"
            }
        }
    }

    private class StreamRef<T>(
        val clazz: Class<*>,
        referent: T,
        q: ReferenceQueue<in T>,
    ) : WeakReference<T>(referent, q)
}