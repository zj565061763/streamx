package com.sd.lib.stream

import android.util.Log
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * 查找[clazz]的所有流接口
 */
@Suppress("UNCHECKED_CAST")
internal fun findStreamInterface(clazz: Class<*>): Collection<Class<out FStream>> {
    clazz.requireIsClass()

    val collection: MutableSet<Class<out FStream>> = hashSetOf()
    var current: Class<*> = clazz

    while (FStream::class.java.isAssignableFrom(current)) {
        val interfaces = current.interfaces
        if (interfaces.isEmpty()) break

        for (item in interfaces) {
            if (FStream::class.java == item) continue
            if (FStream::class.java.isAssignableFrom(item)) {
                collection.add(item as Class<out FStream>)
            }
        }

        current = current.superclass ?: break
    }

    return collection
}

private fun Class<*>.requireIsClass() {
    require(!Proxy.isProxyClass(this)) { "class should not be proxy" }
    require(!Modifier.isInterface(modifiers)) { "class should not be an interface" }
    require(!Modifier.isAbstract(modifiers)) { "class should not be abstract" }
}

internal inline fun logMsg(block: () -> String) {
    if (FStreamManager.isDebug) {
        Log.i("FStream", block())
    }
}