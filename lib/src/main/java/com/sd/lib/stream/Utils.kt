package com.sd.lib.stream.utils

import android.util.Log
import com.sd.lib.stream.FStream
import com.sd.lib.stream.FStreamManager
import java.lang.reflect.Proxy

/**
 * 查找[clazz]的所有流接口
 */
internal fun findStreamClass(clazz: Class<*>): Collection<Class<out FStream>> {
    require(!Proxy.isProxyClass(clazz)) { "proxy class is not supported" }
    val collection = HashSet<Class<out FStream>>()

    var current = clazz
    while (FStream::class.java.isAssignableFrom(current)) {
        if (current.isInterface) {
            throw RuntimeException("class must not be an interface")
        }

        for (item in current.interfaces) {
            if (FStream::class.java.isAssignableFrom(item)) {
                collection.add(item as Class<out FStream>)
            }
        }

        current = current.superclass ?: break
    }

    if (collection.isEmpty()) {
        throw RuntimeException("stream class was not found in ${clazz}")
    }
    return collection
}

internal inline fun logMsg(tag: String = "FStream", block: () -> String) {
    if (FStreamManager.isDebug) {
        Log.i(tag, block())
    }
}