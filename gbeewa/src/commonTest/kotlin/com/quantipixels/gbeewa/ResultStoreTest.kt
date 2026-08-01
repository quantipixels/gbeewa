package com.quantipixels.gbeewa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultStoreTest {

    private object CountryKey : ResultKey<String>
    private object IndexKey : ResultKey<Int>
    private object FirstKey : ResultKey<String>
    private object SecondKey : ResultKey<String>
    private object NullableKey : ResultKey<String?>
    private data class RequestKey(val requestId: String) : ResultKey<String>

    @Test
    fun `consumer delivers a pending result once`() {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        val delivered = mutableListOf<String>()

        state.put(CountryKey, "NG")

        assertTrue(CountryKey in state)
        assertTrue(CountryKey in consumer)
        consumer.consume(CountryKey, delivered::add)

        assertEquals(listOf("NG"), delivered)
        assertFalse(CountryKey in state)
        assertFalse(CountryKey in consumer)

        consumer.consume(CountryKey, delivered::add)
        assertEquals(listOf("NG"), delivered)
    }

    @Test
    fun `latest result replaces an earlier result for the same key`() {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        var delivered: Int? = null

        state.put(IndexKey, 1)
        state.put(IndexKey, 2)
        consumer.consume(IndexKey) { delivered = it }

        assertEquals(2, delivered)
    }

    @Test
    fun `different keys keep independent pending results`() {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        val delivered = mutableListOf<String>()

        state.put(FirstKey, "A")
        state.put(SecondKey, "B")
        consumer.consume(SecondKey, delivered::add)
        consumer.consume(FirstKey, delivered::add)

        assertEquals(listOf("B", "A"), delivered)
    }

    @Test
    fun `nullable key distinguishes a present null from absence`() {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        var calls = 0
        var delivered: String? = "not-null"

        state.put(NullableKey, "value")
        consumer.consume(NullableKey) {
            calls += 1
            delivered = it
        }
        assertEquals(1, calls)
        assertEquals("value", delivered)

        state.put(NullableKey, null)
        assertTrue(NullableKey in consumer)

        consumer.consume(NullableKey) {
            calls += 1
            delivered = it
        }

        assertEquals(2, calls)
        assertNull(delivered)
        consumer.consume(NullableKey) { calls += 1 }
        assertEquals(2, calls)
    }

    @Test
    fun `result is removed before callback execution and callback failure does not restore it`() {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        state.put(CountryKey, "NG")

        assertFailsWith<IllegalStateException> {
            consumer.consume(CountryKey) {
                assertFalse(CountryKey in consumer)
                error("callback failed")
            }
        }

        assertFalse(CountryKey in consumer)
    }

    @Test
    fun `parameterized key identity isolates overlapping requests`() {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        val delivered = mutableListOf<String>()
        val firstRequest = RequestKey("first")
        val secondRequest = RequestKey("second")

        state.put(firstRequest, "A")
        state.put(secondRequest, "B")
        consumer.consume(secondRequest, delivered::add)
        consumer.consume(firstRequest, delivered::add)

        assertEquals(listOf("B", "A"), delivered)
    }

    @Test
    fun `a new owner starts without results from an earlier owner`() {
        val earlierState = ResultStore()
        earlierState.put(CountryKey, "NG")

        val newState = ResultStore()

        assertFalse(CountryKey in newState)
        assertTrue(CountryKey in earlierState)
    }
}
