package com.sd.lib.stream

import android.app.Activity
import android.view.View
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * 流接口
 */
interface FStream {
    /**
     * 返回当前流对象的tag
     *
     * 代理对象方法被触发的时候，会调用流对象的这个方法返回一个tag用于和代理对象的tag比较，tag相等的流对象才会被通知
     *
     * @param clazz 哪个接口的代理对象方法被触发
     */
    fun getTagForStream(clazz: Class<out FStream>): Any?

    class ProxyBuilder {
        internal var streamClass: Class<out FStream>? = null
            private set

        internal var tag: Any? = null
            private set

        internal var beforeDispatchCallback: BeforeDispatchCallback? = null
            private set

        internal var afterDispatchCallback: AfterDispatchCallback? = null
            private set

        internal var resultFilter: ResultFilter? = null
            private set

        /**
         * 设置代理对象的tag
         */
        fun setTag(tag: Any?) = apply {
            this.tag = tag
        }

        /**
         * 设置流对象方法被通知之前回调
         */
        fun setBeforeDispatchCallback(callback: BeforeDispatchCallback?) = apply {
            this.beforeDispatchCallback = callback
        }

        /**
         * 设置流对象方法被通知之后回调
         */
        fun setAfterDispatchCallback(callback: AfterDispatchCallback?) = apply {
            this.afterDispatchCallback = callback
        }

        /**
         * 设置返回值过滤对象
         */
        fun setResultFilter(filter: ResultFilter?) = apply {
            this.resultFilter = filter
        }

        /**
         * 创建代理对象
         */
        fun <T : FStream> build(clazz: Class<T>): T {
            require(clazz.isInterface) { "clazz must be an interface" }
            require(clazz != FStream::class.java) { "clazz must not be:${FStream::class.java.name}" }

            this.streamClass = clazz

            val handler = ProxyInvocationHandler(this)
            val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf<Class<*>>(clazz), handler)
            return proxy as T
        }
    }

    fun interface BeforeDispatchCallback {
        /**
         * 流对象方法被通知之前触发
         *
         * @param stream       流对象
         * @param method       方法
         * @param methodParams 方法参数
         * @return true-停止分发，false-继续分发
         */
        fun dispatch(stream: FStream, method: Method, methodParams: Array<Any?>?): Boolean
    }

    fun interface AfterDispatchCallback {
        /**
         * 流对象方法被通知之后触发
         *
         * @param stream       流对象
         * @param method       方法
         * @param methodParams 方法参数
         * @param methodResult 流对象方法被调用后的返回值
         * @return true-停止分发，false-继续分发
         */
        fun dispatch(stream: FStream, method: Method, methodParams: Array<Any?>?, methodResult: Any?): Boolean
    }

    fun interface ResultFilter {
        /**
         * 过滤返回值
         *
         * @param method       方法
         * @param methodParams 方法参数
         * @param results      所有流对象的返回值
         * @return 返回选定的返回值
         */
        fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any?
    }

    companion object {
        /**
         * 创建代理对象
         */
        @JvmOverloads
        @JvmStatic
        fun <T : FStream> buildProxy(clazz: Class<T>, block: (ProxyBuilder.() -> Unit)? = null): T {
            val builder = ProxyBuilder()
            block?.invoke(builder)
            return builder.build(clazz)
        }
    }
}

/**
 * 创建代理对象
 */
fun <T : FStream> KClass<T>.buildProxy(block: FStream.ProxyBuilder.() -> Unit = {}): T {
    return FStream.buildProxy(this.java, block)
}

/**
 * 注册[FStreamManager.register]
 */
fun FStream.registerStream(): StreamConnection {
    return FStreamManager.register(this)
}

/**
 * 取消注册[FStreamManager.unregister]
 */
fun FStream.unregisterStream() {
    FStreamManager.unregister(this)
}

/**
 * [FStreamManager.bindActivity]
 */
fun FStream.bindActivity(activity: Activity): Boolean {
    return FStreamManager.bindActivity(this, activity)
}

/**
 * [FStreamManager.bindView]
 */
fun FStream.bindView(view: View): Boolean {
    return FStreamManager.bindView(this, view)
}