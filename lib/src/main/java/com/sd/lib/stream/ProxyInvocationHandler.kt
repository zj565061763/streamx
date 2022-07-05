package com.sd.lib.stream

import android.util.Log
import com.sd.lib.stream.FStream.*
import com.sd.lib.stream.utils.logMsg
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.*

internal class ProxyInvocationHandler(builder: ProxyBuilder) : InvocationHandler {
    private val _streamClass: Class<out FStream>
    private val _tag: Any?
    private val _beforeDispatchCallback: BeforeDispatchCallback?
    private val _afterDispatchCallback: AfterDispatchCallback?
    private val _resultFilter: ResultFilter?

    init {
        _streamClass = requireNotNull(builder.streamClass)
        _tag = builder.tag
        _beforeDispatchCallback = builder.beforeDispatchCallback
        _afterDispatchCallback = builder.afterDispatchCallback
        _resultFilter = builder.resultFilter
    }

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val returnType = method.returnType
        val parameterTypes = method.parameterTypes

        if ("getTagForStream" == method.name &&
            parameterTypes.size == 1 &&
            parameterTypes[0] == Class::class.java
        ) {
            return _tag
        }


        val uuid = if (FStreamManager.isDebug) UUID.randomUUID().toString() else ""
        val isVoid = returnType == Void.TYPE || returnType == Void::class.java
        var result = processMainLogic(isVoid, method, args, uuid)


        if (isVoid) {
            result = null
        } else if (returnType.isPrimitive && result == null) {
            result = if (Boolean::class.javaPrimitiveType == returnType) {
                false
            } else {
                0
            }
            logMsg { "return type:${returnType} but method result is null, so set to $result uuid:${uuid}" }
        }

        logMsg { "notify finish return:${result} uuid:${uuid}" }
        return result
    }

    private fun processMainLogic(isVoid: Boolean, method: Method, args: Array<Any?>?, uuid: String?): Any? {
        val listStream = FStreamManager.getStreamHolder(_streamClass)?.toCollection() ?: listOf()

        if (FStreamManager.isDebug) {
            Log.i(
                FStream::class.java.simpleName, "notify -----> $method"
                        + " arg:${(if (args == null) "" else Arrays.toString(args))}"
                        + " tag:${_tag}"
                        + " count:${listStream.size}"
                        + " uuid:${uuid}"
            )
        }

        if (listStream.isEmpty()) {
            // 尝试创建默认流对象
            val defaultStream = DefaultStreamManager.getStream(_streamClass) ?: return null
            val result = if (args != null) {
                method.invoke(defaultStream, *args)
            } else {
                method.invoke(defaultStream)
            }
            logMsg {
                val returnLog = if (isVoid) "" else "return:${result}"
                "notify default stream:${defaultStream} ${returnLog} uuid:${uuid}"
            }
            return result
        }

        val filterResult = _resultFilter != null && !isVoid
        val listResult: MutableList<Any?>? = if (filterResult) LinkedList() else null

        var result: Any? = null
        var index = 0
        for (stream in listStream) {
            val connection = FStreamManager.getConnection(stream)
            if (connection == null) {
                logMsg { "${StreamConnection::class.java.simpleName} is null uuid:${uuid}" }
                continue
            }

            if (stream.getTagForStream(_streamClass) != _tag) {
                continue
            }

            if (_beforeDispatchCallback?.dispatch(stream, method, args) == true) {
                logMsg { "dispatch broken before uuid:${uuid}" }
                break
            }

            var itemResult: Any?
            var shouldBreakDispatch: Boolean

            connection.getItem(_streamClass).let { item ->
                synchronized(item) {
                    item.resetBreakDispatch()

                    // 调用流对象方法
                    itemResult = if (args != null) {
                        method.invoke(stream, *args)
                    } else {
                        method.invoke(stream)
                    }

                    shouldBreakDispatch = item.shouldBreakDispatch
                    item.resetBreakDispatch()
                }
            }

            if (FStreamManager.isDebug) {
                Log.i(
                    FStream::class.java.simpleName, "notify"
                            + " index:${index}"
                            + " return:${if (isVoid) "" else itemResult}"
                            + " stream:$${stream}"
                            + " shouldBreakDispatch:${shouldBreakDispatch}"
                            + " uuid:${uuid}"
                )
            }

            result = itemResult
            if (filterResult) {
                listResult!!.add(itemResult)
            }

            if (_afterDispatchCallback?.dispatch(stream, method, args, itemResult) == true) {
                logMsg { "dispatch broken after uuid:${uuid}" }
                break
            }

            if (shouldBreakDispatch) {
                break
            }

            index++
        }

        if (filterResult && listResult!!.isNotEmpty()) {
            result = _resultFilter!!.filter(method, args, listResult)
            logMsg { "proxy filter result:${result} uuid:${uuid}" }
        }
        return result
    }
}