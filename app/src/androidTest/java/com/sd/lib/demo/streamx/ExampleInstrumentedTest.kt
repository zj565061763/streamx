package com.sd.lib.demo.streamx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sd.lib.demo.streamx.utils.TestBuildStream
import com.sd.lib.demo.streamx.utils.TestDefaultStream
import com.sd.lib.demo.streamx.utils.TestStream
import com.sd.lib.stream.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testRegisterUnregister() {
        val stream0 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(0)
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream1 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(1)
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        stream1.registerStream()

        Assert.assertNotNull(FStreamManager.getConnection(stream0))
        Assert.assertNotNull(FStreamManager.getConnection(stream1))

        val builder = StringBuilder()
        val proxy = TestBuildStream::class.buildProxy()

        proxy.build(builder)
        Assert.assertEquals("01", builder.toString())
        builder.clear()

        stream0.unregisterStream()
        proxy.build(builder)
        Assert.assertEquals("1", builder.toString())
        builder.clear()

        stream1.unregisterStream()
        proxy.build(builder)
        Assert.assertEquals("", builder.toString())
        builder.clear()

        Assert.assertNull(FStreamManager.getConnection(stream0))
        Assert.assertNull(FStreamManager.getConnection(stream1))
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

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        stream1.registerStream()

        val builder = StringBuilder()
        val proxy = TestBuildStream::class.buildProxy() {
            setTag("none null tag")
        }

        proxy.build(builder)
        Assert.assertEquals("0", builder.toString())

        stream0.unregisterStream()
        stream1.unregisterStream()
    }

    @Test
    fun testDefaultStream() {
        val proxy = TestStream::class.buildProxy()
        DefaultStreamManager.register(TestDefaultStream::class.java)

        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "0"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        Assert.assertEquals("0", proxy.getContent("http"))

        stream0.unregisterStream()
        Assert.assertEquals("default@http", proxy.getContent("http"))

        DefaultStreamManager.unregister(TestDefaultStream::class.java)
        Assert.assertEquals(null, proxy.getContent("http"))
    }

    @Test
    fun testPriority() {
        val stream0 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(0)
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream1 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(1)
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream2 = object : TestBuildStream {
            override fun build(builder: StringBuilder) {
                builder.append(2)
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        stream0.registerStream().setPriority(-1)
        stream1.registerStream().setPriority(1)
        stream2.registerStream()

        Assert.assertEquals(-1, FStreamManager.getConnection(stream0)!!.getPriority())
        Assert.assertEquals(1, FStreamManager.getConnection(stream1)!!.getPriority())
        Assert.assertEquals(0, FStreamManager.getConnection(stream2)!!.getPriority())

        val builder = StringBuilder()
        val proxy = TestBuildStream::class.buildProxy()

        proxy.build(builder)
        Assert.assertEquals("120", builder.toString())

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testBeforeDispatchCallback() {
        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "0"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()

        val proxy = TestStream::class.buildProxy {
            setBeforeDispatchCallback { _, _, _ ->
                true
            }
        }

        val result = proxy.getContent("http")
        Assert.assertEquals(null, result)

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testAfterDispatchCallback() {
        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "0"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()


        val proxy = TestStream::class.buildProxy {
            setAfterDispatchCallback { _, _, _, methodResult ->
                "1" == methodResult
            }
        }


        val result = proxy.getContent("http")
        Assert.assertEquals("1", result)

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testResultFilter() {
        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "0"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }
        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        stream1.registerStream()
        stream2.registerStream()

        val proxy = TestStream::class.buildProxy {
            setResultFilter { _, _, results ->
                Assert.assertEquals(3, results.size)
                Assert.assertEquals("0", results[0])
                Assert.assertEquals("1", results[1])
                Assert.assertEquals("2", results[2])
                results[1]
            }
        }

        val result = proxy.getContent("http")
        Assert.assertEquals("1", result)

        stream0.unregisterStream()
        stream1.unregisterStream()
        stream2.unregisterStream()
    }

    @Test
    fun testDispatchBreak() {
        val stream1 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "1"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream2 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                FStreamManager.getConnection(this)!!.breakDispatch(TestStream::class.java)
                return "2"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        FStreamManager.run {
            this.register(stream1)
            this.register(stream2)
            this.register(stream3)
        }

        val listResult = mutableListOf<Any?>()
        val proxy = FStream.ProxyBuilder()
            .setResultFilter(object : FStream.ResultFilter {
                override fun filter(method: Method, methodParams: Array<Any?>?, results: List<Any?>): Any? {
                    listResult.addAll(results)
                    return results.last()
                }
            })
            .build(TestStream::class.java)

        val result = proxy.getContent("http")
        Assert.assertEquals("2", result)
        Assert.assertEquals(2, listResult.size)
        Assert.assertEquals("1", listResult[0])
        Assert.assertEquals("2", listResult[1])

        FStreamManager.run {
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
        }
    }
}