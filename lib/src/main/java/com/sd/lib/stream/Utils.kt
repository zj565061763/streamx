package com.sd.lib.stream

import android.util.Log
import java.lang.reflect.Proxy

/**
 * 查找[clazz]的所有流接口
 */
internal fun findStreamInterface(clazz: Class<*>): Collection<Class<out FStream>> {
    require(!Proxy.isProxyClass(clazz)) { "class should not be proxy" }
    require(!clazz.isInterface) { "class should not be an interface" }

    val collection = HashSet<Class<out FStream>>()
    var current = clazz

    while (FStream::class.java.isAssignableFrom(current)) {
        val interfaces = current.interfaces
        if (interfaces.isEmpty()) break

        for (item in interfaces) {
            if (FStream::class.java.isAssignableFrom(item)) {
                collection.add(item as Class<out FStream>)
            }
        }

        current = current.superclass ?: break
    }

    check(collection.isNotEmpty()) { "stream interface was not found in $clazz" }
    return collection
}

internal inline fun logMsg(tag: String = "FStream", block: () -> String) {
    if (FStreamManager.isDebug) {
        Log.i(tag, block())
    }
}