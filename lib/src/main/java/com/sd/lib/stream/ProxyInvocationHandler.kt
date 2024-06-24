package com.sd.lib.stream

import com.sd.lib.stream.FStream.AfterDispatchCallback
import com.sd.lib.stream.FStream.BeforeDispatchCallback
import com.sd.lib.stream.FStream.ProxyBuilder
import com.sd.lib.stream.FStream.ResultFilter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.UUID

internal class ProxyInvocationHandler(builder: ProxyBuilder) : InvocationHandler {
    private val _streamClass: Class<out FStream> = builder.streamClass
    private val _tag: Any? = builder.tag
    private val _beforeDispatchCallback: BeforeDispatchCallback? = builder.beforeDispatchCallback
    private val _afterDispatchCallback: AfterDispatchCallback? = builder.afterDispatchCallback
    private val _resultFilter: ResultFilter? = builder.resultFilter

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
        var result = processMainLogic(
            isVoid = isVoid,
            method = method,
            args = args,
            uuid = uuid,
        )


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

        logMsg { "notify ----- return:${result} uuid:${uuid}" }
        return result
    }

    private fun processMainLogic(
        isVoid: Boolean,
        method: Method,
        args: Array<Any?>?,
        uuid: String?,
    ): Any? {
        val listStream = FStreamManager.getStreams(_streamClass)
        logMsg {
            buildString {
                append("notify +++++ $method")
                if (!args.isNullOrEmpty()) {
                    append(" arg:${args.contentToString()}")
                }
                append(" tag:${_tag}")
                append(" count:${listStream?.size ?: 0}")
                append(" uuid:${uuid}")
            }
        }
        if (listStream.isNullOrEmpty()) {
            return null
        }

        val filterResult = _resultFilter != null && !isVoid
        val listResult: MutableList<Any?>? = if (filterResult) ArrayList(listStream.size) else null

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

            var itemResult: Any? = null
            var itemBreakDispatch = false

            if (connection.isConnected()) {
                connection.getItem(_streamClass).let { item ->
                    synchronized(item) {
                        item.resetBreakDispatch()

                        // 调用流对象方法
                        itemResult = if (args != null) {
                            method.invoke(stream, *args)
                        } else {
                            method.invoke(stream)
                        }

                        itemBreakDispatch = item.shouldBreakDispatch
                        item.resetBreakDispatch()
                    }

                    logMsg {
                        buildString {
                            append("notify")
                            append(" (${index})")
                            if (!isVoid) {
                                append(" -> (${itemResult})")
                            }
                            append(" $stream")
                            append(" break:${itemBreakDispatch}")
                            append(" uuid:${uuid}")
                        }
                    }

                    result = itemResult.also {
                        if (filterResult) {
                            listResult!!.add(it)
                        }
                    }
                }
            } else {
                logMsg { "connection is disconnected $stream uuid:${uuid}" }
            }

            if (_afterDispatchCallback?.dispatch(stream, method, args, itemResult) == true) {
                logMsg { "dispatch broken after uuid:${uuid}" }
                break
            }

            if (itemBreakDispatch) {
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