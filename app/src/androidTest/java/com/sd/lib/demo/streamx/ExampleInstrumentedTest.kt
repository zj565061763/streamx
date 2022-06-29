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
    fun testNormal() {
        DefaultStreamManager.register(TestDefaultStream::class.java)

        val stream0 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "0"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return "hello tag"
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

        val stream3 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "3"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                return null
            }
        }

        stream0.registerStream()
        stream1.registerStream().setPriority(-1)
        stream2.registerStream()
        stream3.registerStream().setPriority(1)

        Assert.assertEquals(0, FStreamManager.getConnection(stream0)!!.getPriority(TestStream::class.java))
        Assert.assertEquals(-1, FStreamManager.getConnection(stream1)!!.getPriority(TestStream::class.java))
        Assert.assertEquals(0, FStreamManager.getConnection(stream2)!!.getPriority(TestStream::class.java))
        Assert.assertEquals(1, FStreamManager.getConnection(stream3)!!.getPriority(TestStream::class.java))


        val listResult = mutableListOf<Any?>()
        val proxy = TestStream::class.buildProxy {
            setResultFilter { _, _, results ->
                listResult.clear()
                listResult.addAll(results)
                results.last()
            }
        }


        proxy.getContent("http").let { result ->
            Assert.assertEquals("1", result)
            Assert.assertEquals(3, listResult.size)
            Assert.assertEquals("3", listResult[0])
            Assert.assertEquals("2", listResult[1])
            Assert.assertEquals("1", listResult[2])
        }


        val stream4 = object : TestStream {
            override fun getContent(url: String): String {
                Assert.assertEquals("http", url)
                return "4"
            }

            override fun getTagForStream(clazz: Class<out FStream>): Any? {
                Assert.assertEquals(TestStream::class.java, clazz)
                return null
            }
        }.also { it.registerStream() }


        proxy.getContent("http").let { result ->
            Assert.assertEquals("1", result)
            Assert.assertEquals(4, listResult.size)
            Assert.assertEquals("3", listResult[0])
            Assert.assertEquals("2", listResult[1])
            Assert.assertEquals("4", listResult[2])
            Assert.assertEquals("1", listResult[3])
        }


        DefaultStreamManager.unregister(TestDefaultStream::class.java)
        FStreamManager.run {
            this.unregister(stream0)
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
            this.unregister(stream4)
        }

        Assert.assertEquals(null, FStreamManager.getConnection(stream0))
        Assert.assertEquals(null, FStreamManager.getConnection(stream1))
        Assert.assertEquals(null, FStreamManager.getConnection(stream2))
        Assert.assertEquals(null, FStreamManager.getConnection(stream3))
        Assert.assertEquals(null, FStreamManager.getConnection(stream4))
    }


    @Test
    fun testDispatchCallbackBefore() {
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

        val proxy = FStream.ProxyBuilder()
            .setDispatchCallback(object : FStream.DispatchCallback {
                override fun beforeDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?): Boolean {
                    return true
                }

                override fun afterDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?, methodResult: Any?): Boolean {
                    return false
                }
            })
            .build(TestStream::class.java)

        val result = proxy.getContent("http")
        Assert.assertEquals(null, result)

        FStreamManager.run {
            this.unregister(stream1)
            this.unregister(stream2)
            this.unregister(stream3)
        }
    }

    @Test
    fun testDispatchCallbackAfter() {
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
            .setDispatchCallback(object : FStream.DispatchCallback {
                override fun beforeDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?): Boolean {
                    return false
                }

                override fun afterDispatch(stream: FStream, method: Method, methodParams: Array<Any?>?, methodResult: Any?): Boolean {
                    return "2" == methodResult
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