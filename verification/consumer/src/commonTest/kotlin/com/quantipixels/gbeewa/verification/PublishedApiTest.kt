package com.quantipixels.gbeewa.verification

import com.quantipixels.gbeewa.*
import kotlin.test.*

class PublishedApiTest {
    private object Key : ResultKey<String?>

    @Test fun publishedStoreConsumesPresentNullExactlyOnce() {
        val store = ResultStore()
        val consumer = ResultConsumer(store)
        store.put(Key, null)
        assertTrue(Key in consumer)
        assertEquals(PendingResult.Value(null), consumer.consume(Key))
        assertEquals(PendingResult.Missing, consumer.consume(Key))
    }

    @Test fun oldOwnerRemainsSeparateFromNewOwner() {
        val oldStore = ResultStore()
        val newStore = ResultStore()
        val lateCallback: () -> Unit = { oldStore.put(Key, "late") }
        lateCallback()
        assertEquals(PendingResult.Missing, ResultConsumer(newStore).consume(Key))
        assertEquals(PendingResult.Value("late"), ResultConsumer(oldStore).consume(Key))
    }
}
