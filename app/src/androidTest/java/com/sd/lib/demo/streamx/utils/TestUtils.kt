package com.sd.lib.demo.streamx.utils

import com.sd.lib.stream.FStream

interface TestBuildStream : FStream {
    fun build(builder: StringBuilder)
}

interface TestStream : FStream {
    fun getContent(url: String): String
}

class TestDefaultStream : TestStream {
    override fun getContent(url: String): String {
        return "default@${url}"
    }

    override fun getTagForStream(clazz: Class<out FStream>): Any? {
        return null
    }
}