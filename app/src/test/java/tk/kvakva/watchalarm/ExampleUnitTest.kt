package tk.kvakva.watchalarm

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun xxxx() {
        val x= sqrt(16f.pow(2)/10000*10f.pow(2))
        println("$x")
    }
}