package com.sd.demo.stream

import com.sd.lib.stream.FStream
import com.sd.lib.stream.fStreamProxy
import com.sd.lib.stream.getStreamConnection
import com.sd.lib.stream.registerStream
import com.sd.lib.stream.unregisterStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StreamTest {
    @Test
    fun testRegisterUnregister() {
        val stream0 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(0)
            }
        }
        val stream1 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(1)
            }
        }

        stream0.registerStream()
        stream1.registerStream()

        assertNotNull(stream0.getStreamConnection())
        assertNotNull(stream1.getStreamConnection())

        val builder = StringBuilder()
        val proxy = fStreamProxy<TestBuildStream>()

        proxy.build(builder)
        assertEquals("01", builder.toString())
        builder.clear()

        stream0.unregisterStream()
        proxy.build(builder)
        assertEquals("1", builder.toString())
        builder.clear()

        stream1.unregisterStream()
        proxy.build(builder)
        assertEquals("", builder.toString())
        builder.clear()

        assertNull(stream0.getStreamConnection())
        assertNull(stream1.getStreamConnection())
    }

    @Test
    fun testUnregisterWhenDispatch() {
        val proxy = fStreamProxy<TestStream>()

        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                unregisterStream()
                return "0"
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "1"
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "2"
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()

        proxy.getContent("http")
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testStreamTag() {
        val stream0 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(0)
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any {
                return "none null tag"
            }
        }
        val stream1 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(1)
            }
        }

        stream0.registerStream()
        stream1.registerStream()

        val builder = StringBuilder()
        val proxy = fStreamProxy<TestBuildStream> {
            setTag("none null tag")
        }

        proxy.build(builder)
        assertEquals("0", builder.toString())

        stream0.unregisterStream()
        stream1.unregisterStream()
    }

    @Test
    fun testPriority() {
        val stream0 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(0)
            }
        }
        val stream1 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(1)
            }
        }
        val stream2 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(2)
            }
        }
        stream0.registerStream().setPriority(-1)
        stream1.registerStream().setPriority(1)
        stream2.registerStream()

        assertEquals(-1, stream0.getStreamConnection()!!.getPriority())
        assertEquals(1, stream1.getStreamConnection()!!.getPriority())
        assertEquals(0, stream2.getStreamConnection()!!.getPriority())

        val builder = StringBuilder()
        val proxy = fStreamProxy<TestBuildStream>()

        proxy.build(builder)
        assertEquals("120", builder.toString())

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testBeforeDispatch() {
        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "0"
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "1"
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "2"
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()

        val proxy = fStreamProxy<TestStream> {
            setBeforeDispatch { _, _, _ ->
                true
            }
        }

        val result = proxy.getContent("http")
        assertEquals(null, result)

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testAfterDispatch() {
        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "0"
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "1"
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "2"
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()

        val proxy = fStreamProxy<TestStream> {
            setAfterDispatch { _, _, _, methodResult ->
                "1" == methodResult
            }
        }

        val result = proxy.getContent("http")
        assertEquals("1", result)

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testResultFilter() {
        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "0"
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "1"
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                assertEquals("http", url)
                return "2"
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()

        val proxy = fStreamProxy<TestStream> {
            setResultFilter { _, _, results ->
                assertEquals(3, results.size)
                assertEquals("0", results[0])
                assertEquals("1", results[1])
                assertEquals("2", results[2])
                results[1]
            }
        }

        val result = proxy.getContent("http")
        assertEquals("1", result)

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }
}