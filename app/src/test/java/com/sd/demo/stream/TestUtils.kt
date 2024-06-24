package com.sd.demo.stream

import com.sd.lib.stream.FStream

interface TestBuildStream : FStream {
    fun build(builder: StringBuilder)
}

interface TestStream : FStream {
    fun getContent(url: String): String
}