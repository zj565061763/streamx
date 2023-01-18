package com.sd.lib.demo.streamx.utils

import com.sd.lib.stream.FStream

interface TestBuildStream : FStream {
    fun build(builder: StringBuilder)
}

interface TestStream : FStream {
    fun getContent(url: String): String
}