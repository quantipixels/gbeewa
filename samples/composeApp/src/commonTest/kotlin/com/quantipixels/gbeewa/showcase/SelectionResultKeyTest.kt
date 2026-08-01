package com.quantipixels.gbeewa.showcase

import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectionResultKeyTest {

    @Test
    fun `equal receiver ids address the same result lane`() {
        val state = ResultStore()
        val publishedKey = SelectionResultKey("primary-color")
        val receiverKey = SelectionResultKey("primary-color")
        var received: String? = null

        state.put(publishedKey, "Green")
        ResultConsumer(state).consume(receiverKey) { received = it }

        assertEquals("Green", received)
        assertFalse(receiverKey in state)
    }

    @Test
    fun `different receiver ids retain independent selections`() {
        val state = ResultStore()
        val primaryColorKey = SelectionResultKey("primary-color")
        val accentColorKey = SelectionResultKey("accent-color")
        val received = mutableMapOf<String, String>()

        state.put(primaryColorKey, "Green")
        state.put(accentColorKey, "Orange")

        assertTrue(primaryColorKey in state)
        assertTrue(accentColorKey in state)

        val consumer = ResultConsumer(state)
        consumer.consume(accentColorKey) { received[accentColorKey.receiverId] = it }

        assertTrue(primaryColorKey in state)
        assertFalse(accentColorKey in state)

        consumer.consume(primaryColorKey) { received[primaryColorKey.receiverId] = it }

        assertEquals(
            mapOf(
                "primary-color" to "Green",
                "accent-color" to "Orange",
            ),
            received,
        )
    }
}
