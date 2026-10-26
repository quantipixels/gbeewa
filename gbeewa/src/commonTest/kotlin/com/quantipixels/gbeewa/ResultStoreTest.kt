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
    fun `direct consumption returns a pending result once`() {
        val store = ResultStore()
        val consumer = ResultConsumer(store)

        assertEquals(PendingResult.Missing, consumer.consume(CountryKey))

        store.put(CountryKey, "NG")

        assertEquals(PendingResult.Value("NG"), consumer.consume(CountryKey))
        assertFalse(CountryKey in consumer)
        assertEquals(PendingResult.Missing, consumer.consume(CountryKey))
    }

    @Test
    fun `direct consumption distinguishes a nullable value from a missing result`() {
        val store = ResultStore()
        val consumer = ResultConsumer(store)

        store.put(NullableKey, null)

        assertEquals(PendingResult.Value(null), consumer.consume(NullableKey))
        assertEquals(PendingResult.Missing, consumer.consume(NullableKey))
    }

    @Test
    fun `consume-only cleanup removes a nullable request without callbacks or affecting another lane`() {
        val store = ResultStore()
        val consumer = ResultConsumer(store)
        val delivered = mutableListOf<String?>()
        store.put(NullableKey, null)
        store.put(CountryKey, "NG")

        assertEquals(PendingResult.Value(null), consumer.consume(NullableKey))
        consumer.consume(NullableKey, delivered::add)

        assertTrue(delivered.isEmpty())
        assertEquals(PendingResult.Missing, consumer.consume(NullableKey))
        assertTrue(CountryKey in consumer)
        consumer.consume(CountryKey, delivered::add)
        assertEquals(listOf<String?>("NG"), delivered)
    }

    @Test
    fun `direct consumption returns the latest result for the same key`() {
        val store = ResultStore()
        val consumer = ResultConsumer(store)

        store.put(IndexKey, 1)
        store.put(IndexKey, 2)

        assertEquals(PendingResult.Value(2), consumer.consume(IndexKey))
    }

    @Test
    fun `callback and direct consumers compete for one pending result`() {
        val store = ResultStore()
        val callbackConsumer = ResultConsumer(store)
        val directConsumer = ResultConsumer(store)
        val delivered = mutableListOf<String>()

        store.put(CountryKey, "callback")
        callbackConsumer.consume(CountryKey, delivered::add)

        assertEquals(PendingResult.Missing, directConsumer.consume(CountryKey))
        assertEquals(listOf("callback"), delivered)

        store.put(CountryKey, "direct")

        assertEquals(PendingResult.Value("direct"), directConsumer.consume(CountryKey))
        callbackConsumer.consume(CountryKey, delivered::add)
        assertEquals(listOf("callback"), delivered)
    }

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
