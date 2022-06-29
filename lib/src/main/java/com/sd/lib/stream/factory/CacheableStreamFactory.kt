package com.sd.lib.stream.factory

import com.sd.lib.stream.FStream
import com.sd.lib.stream.factory.DefaultStreamFactory.CreateParam

abstract class CacheableStreamFactory : DefaultStreamFactory {

    @Synchronized
    final override fun create(param: CreateParam): FStream {
        val cache = getCache(param)
        if (cache != null) return cache
        return createStream(param).also {
            setCache(param, it)
        }
    }

    /**
     * 创建Stream对象
     */
    protected open fun createStream(param: CreateParam): FStream {
        return param.classStreamDefault.newInstance()
    }

    /**
     * 获取缓存
     */
    protected abstract fun getCache(param: CreateParam): FStream?

    /**
     * 设置缓存
     */
    protected abstract fun setCache(param: CreateParam, stream: FStream)
}