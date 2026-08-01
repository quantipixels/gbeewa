package com.quantipixels.gbeewa

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ResultTypeContractAndroidTest {

    private object StringKey : ResultKey<String>

    @Test
    fun `typed contract bypass fails visibly after removal`() {
        val store = ResultStore()
        val consumer = ResultConsumer(store)
        @Suppress("UNCHECKED_CAST")
        val bypassedKey = StringKey as ResultKey<Int>
        store.put(bypassedKey, 7)

        assertFailsWith<ClassCastException> {
            consumer.consume<String>(StringKey) { it.length }
        }

        assertFalse(StringKey in consumer)
    }
}
