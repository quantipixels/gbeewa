package com.quantipixels.gbeewa

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResultEffectTest {

    private object StringKey : ResultKey<String>
    private object OtherStringKey : ResultKey<String>
    private object NullableKey : ResultKey<String?>
    private object CustomValueKey : ResultKey<CustomValue>

    private data class CustomValue(val text: String)

    @Test
    fun `missing provider fails with a clear error`() = runCompositionTest {
        val error = assertFailsWith<IllegalStateException> {
            setContent {
                LocalResultConsumer.current
            }
        }

        assertTrue(error.message.orEmpty().contains("LocalResultConsumer"))
        assertTrue(error.message.orEmpty().contains("CompositionLocalProvider"))
    }

    @Test
    fun `result published before composition is delivered once`() = runCompositionTest {
        val state = ResultStore()
        val delivered = mutableListOf<String>()
        state.put(StringKey, "before")

        setContent(state) {
            ResultEffect(StringKey, delivered::add)
        }
        settle()
        settle()

        assertEquals(listOf("before"), delivered)
        assertFalse(StringKey in state)
    }

    @Test
    fun `result published while mounted is delivered once without replay`() = runCompositionTest {
        val state = ResultStore()
        val delivered = mutableListOf<String>()
        val unrelatedState = mutableStateOf(0)
        setContent(state) {
            unrelatedState.value
            ResultEffect(StringKey, delivered::add)
        }
        settle()

        state.put(StringKey, "mounted")
        settle()
        unrelatedState.value += 1
        settle()

        assertEquals(listOf("mounted"), delivered)
    }

    @Test
    fun `pending effect uses callback replaced before its coroutine runs`() = runCompositionTest {
        val state = ResultStore()
        val consumer = ResultConsumer(state)
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()
        val firstCallback = RecordingCallback(first)
        val secondCallback = RecordingCallback(second)
        val callbackVersion = mutableStateOf(0)
        var composedCallbackVersion = -1
        state.put(StringKey, "pending")

        val content: @Composable () -> Unit = {
            composedCallbackVersion = callbackVersion.value
            val callback = if (callbackVersion.value == 0) firstCallback else secondCallback
            ResultEffect(StringKey, callback)
        }
        setContent(consumer, content)
        assertTrue(StringKey in state)

        callbackVersion.value = 1
        state.put(StringKey, "replacement")
        setContent(consumer, content)
        assertEquals(1, composedCallbackVersion)
        assertTrue(StringKey in state)
        settle()

        assertTrue(first.isEmpty())
        assertEquals(listOf("replacement"), second)
    }

    @Test
    fun `changing the key delivers the pending result for the new key`() = runCompositionTest {
        val state = ResultStore()
        val activeKey = mutableStateOf<ResultKey<String>>(StringKey)
        val delivered = mutableListOf<String>()
        state.put(OtherStringKey, "other")
        setContent(state) {
            ResultEffect(activeKey.value, delivered::add)
        }
        settle()

        activeKey.value = OtherStringKey
        settle()

        assertEquals(listOf("other"), delivered)
    }

    @Test
    fun `pending effect restarts for consumer replaced before its coroutine runs`() =
        runCompositionTest {
            val firstState = ResultStore()
            val secondState = ResultStore()
            val firstConsumer = ResultConsumer(firstState)
            val secondConsumer = ResultConsumer(secondState)
            val activeConsumer = mutableStateOf(firstConsumer)
            val delivered = mutableListOf<String>()
            firstState.put(StringKey, "first")
            secondState.put(StringKey, "second")

            setContent {
                CompositionLocalProvider(LocalResultConsumer provides activeConsumer.value) {
                    ResultEffect(StringKey, delivered::add)
                }
            }

            activeConsumer.value = secondConsumer
            recomposeWithoutRunningEffects()
            settle()

            assertEquals(listOf("second"), delivered)
            assertTrue(StringKey in firstState)
            assertFalse(StringKey in secondState)
        }

    @Test
    fun `two mounted effects deliver one pending result at most once`() = runCompositionTest {
        val state = ResultStore()
        val delivered = mutableListOf<String>()
        state.put(StringKey, "one")
        setContent(state) {
            ResultEffect(StringKey) { delivered += "first:$it" }
            ResultEffect(StringKey) { delivered += "second:$it" }
        }
        settle()

        assertEquals(1, delivered.size)
        assertTrue(delivered.single() in setOf("first:one", "second:one"))
    }

    @Test
    fun `nullable effect delivers a present null`() = runCompositionTest {
        val state = ResultStore()
        var calls = 0
        var delivered: String? = "not-null"
        state.put(NullableKey, null)
        setContent(state) {
            ResultEffect(NullableKey) {
                calls += 1
                delivered = it
            }
        }
        settle()

        assertEquals(1, calls)
        assertEquals(null, delivered)
    }

    @Test
    fun `leaving composition does not discard a later pending result`() = runCompositionTest {
        val state = ResultStore()
        val delivered = mutableListOf<String>()
        setContent(state) {
            ResultEffect(StringKey, delivered::add)
        }
        settle()

        setContent {}
        settle()
        state.put(StringKey, "retained")
        settle()
        assertTrue(StringKey in state)

        setContent(state) {
            ResultEffect(StringKey, delivered::add)
        }
        settle()

        assertEquals(listOf("retained"), delivered)
    }

    @Test
    fun `remembered result state starts with initial value and updates from its result key`() =
        runCompositionTest {
            val state = ResultStore()
            var observed = "unset"
            setContent(state) {
                observed = rememberResultAsState(StringKey) { "initial" }.value
            }
            settle()
            assertEquals("initial", observed)

            state.put(StringKey, "result")
            settle()

            assertEquals("result", observed)
        }

    @Test
    fun `remembered result initializer does not run again on ordinary recomposition`() =
        runCompositionTest {
            val state = ResultStore()
            val unrelatedState = mutableStateOf(0)
            var initializerCalls = 0
            setContent(state) {
                unrelatedState.value
                rememberResultAsState(StringKey) {
                    initializerCalls += 1
                    "initial"
                }
            }
            settle()

            unrelatedState.value += 1
            settle()

            assertEquals(1, initializerCalls)
        }

    @Test
    fun `changing result key creates new initial state without caching the prior key`() =
        runCompositionTest {
            val state = ResultStore()
            val activeKey = mutableStateOf<ResultKey<String>>(StringKey)
            var initialValue = "first-initial"
            var observed = "unset"
            setContent(state) {
                observed = rememberResultAsState(activeKey.value) { initialValue }.value
            }
            settle()
            state.put(StringKey, "first-result")
            settle()
            assertEquals("first-result", observed)

            initialValue = "other-initial"
            activeKey.value = OtherStringKey
            settle()
            assertEquals("other-initial", observed)

            initialValue = "first-new-initial"
            activeKey.value = StringKey
            settle()
            assertEquals("first-new-initial", observed)
        }

    @Test
    fun `changing an extra remember key creates new initial state`() = runCompositionTest {
        val state = ResultStore()
        val userId = mutableStateOf("user-1")
        var observed = "unset"
        setContent(state) {
            observed = rememberResultAsState(StringKey, userId.value) {
                "initial:${userId.value}"
            }.value
        }
        settle()
        state.put(StringKey, "result")
        settle()
        assertEquals("result", observed)

        userId.value = "user-2"
        settle()

        assertEquals("initial:user-2", observed)
    }

    @Test
    fun `helper and direct effect for one key still deliver one result at most once`() =
        runCompositionTest {
            val state = ResultStore()
            val directDeliveries = mutableListOf<String>()
            var helperValue = "initial"
            state.put(StringKey, "result")
            setContent(state) {
                helperValue = rememberResultAsState(StringKey) { "initial" }.value
                ResultEffect(StringKey, directDeliveries::add)
            }
            settle()

            val helperDeliveries = if (helperValue == "result") 1 else 0
            assertEquals(1, helperDeliveries + directDeliveries.size)
            assertFalse(StringKey in state)
        }

    @Test
    fun `saveable result state restores the delivered value without replaying consumption`() =
        runCompositionTest {
            val state = ResultStore()
            val firstRegistry = SaveableStateRegistry(null) { true }
            var observed = "unset"
            var initialValue = "initial"
            val content: @Composable () -> Unit = {
                observed = rememberSaveableResultAsState(StringKey) { initialValue }.value
            }
            setContent(state, firstRegistry, content)
            settle()

            state.put(StringKey, "delivered")
            settle()
            assertEquals("delivered", observed)
            assertFalse(StringKey in state)
            val savedValues = firstRegistry.performSave()

            setContent {}
            settle()
            initialValue = "new-initial"
            val restoredRegistry = SaveableStateRegistry(savedValues) { true }
            setContent(state, restoredRegistry, content)
            settle()

            assertEquals("delivered", observed)
            assertFalse(StringKey in state)
        }

    @Test
    fun `saveable result state works without a saveable registry`() = runCompositionTest {
        val state = ResultStore()
        var observed = "unset"
        setContent(state) {
            observed = rememberSaveableResultAsState(StringKey) { "initial" }.value
        }
        settle()
        assertEquals("initial", observed)

        state.put(StringKey, "delivered")
        settle()

        assertEquals("delivered", observed)
    }

    @Test
    fun `saveable result state restores a delivered null`() = runCompositionTest {
        val state = ResultStore()
        val firstRegistry = SaveableStateRegistry(null) { true }
        var observed: String? = "unset"
        var initialValue: String? = "initial"
        val content: @Composable () -> Unit = {
            observed = rememberSaveableResultAsState(NullableKey) { initialValue }.value
        }
        setContent(state, firstRegistry, content)
        settle()

        state.put(NullableKey, null)
        settle()
        val savedValues = firstRegistry.performSave()

        setContent {}
        settle()
        initialValue = "new-initial"
        val restoredRegistry = SaveableStateRegistry(savedValues) { true }
        setContent(state, restoredRegistry, content)
        settle()

        assertEquals(null, observed)
    }

    @Test
    fun `saveable result state restores through a custom saver`() = runCompositionTest {
        val state = ResultStore()
        val saver = Saver<CustomValue, String>(
            save = { it.text },
            restore = { CustomValue(it) },
        )
        val canBeSaved: (Any) -> Boolean = {
            it is String || it is MutableState<*> && it.value is String
        }
        val firstRegistry = SaveableStateRegistry(null, canBeSaved)
        var observed = CustomValue("unset")
        var initialValue = CustomValue("initial")
        val content: @Composable () -> Unit = {
            observed = rememberSaveableResultAsState(
                resultKey = CustomValueKey,
                saver = saver,
            ) { initialValue }.value
        }
        setContent(state, firstRegistry, content)
        settle()

        state.put(CustomValueKey, CustomValue("delivered"))
        settle()
        val savedValues = firstRegistry.performSave()

        setContent {}
        settle()
        initialValue = CustomValue("new-initial")
        val restoredRegistry = SaveableStateRegistry(savedValues, canBeSaved)
        setContent(state, restoredRegistry, content)
        settle()

        assertEquals(CustomValue("delivered"), observed)
    }

    @Test
    fun `custom saver result state works without a saveable registry`() = runCompositionTest {
        val state = ResultStore()
        val saver = Saver<CustomValue, String>(
            save = { it.text },
            restore = { CustomValue(it) },
        )
        var observed = CustomValue("unset")
        setContent(state) {
            observed = rememberSaveableResultAsState(
                resultKey = CustomValueKey,
                saver = saver,
            ) { CustomValue("initial") }.value
        }
        settle()
        assertEquals(CustomValue("initial"), observed)

        state.put(CustomValueKey, CustomValue("delivered"))
        settle()

        assertEquals(CustomValue("delivered"), observed)
    }

    @Test
    fun `changing a saveable input creates new initial state`() = runCompositionTest {
        val state = ResultStore()
        val registry = SaveableStateRegistry(null) { true }
        val userId = mutableStateOf("user-1")
        var observed = "unset"
        setContent(state, registry) {
            observed = rememberSaveableResultAsState(StringKey, userId.value) {
                "initial:${userId.value}"
            }.value
        }
        settle()
        state.put(StringKey, "delivered")
        settle()
        assertEquals("delivered", observed)

        userId.value = "user-2"
        settle()

        assertEquals("initial:user-2", observed)
    }

    private fun runCompositionTest(block: suspend CompositionFixture.() -> Unit): TestResult = runTest {
        val frameClock = BroadcastFrameClock()
        val effectDispatcher = StandardTestDispatcher(testScheduler)
        val recomposer = Recomposer(effectDispatcher + frameClock)
        val composition = Composition(UnitApplier(), recomposer)
        val recomposerJob = backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler) + frameClock,
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }
        val fixture = CompositionFixture(
            scope = this,
            composition = composition,
            frameClock = frameClock,
            recomposerJob = recomposerJob,
        )

        try {
            fixture.block()
        } finally {
            composition.dispose()
            recomposer.cancel()
            recomposerJob.cancel()
        }
    }

    private class CompositionFixture(
        private val scope: TestScope,
        private val composition: Composition,
        private val frameClock: BroadcastFrameClock,
        @Suppress("unused") private val recomposerJob: Job,
    ) {
        fun setContent(content: @Composable () -> Unit) {
            composition.setContent(content)
        }

        fun setContent(state: ResultStore, content: @Composable () -> Unit) {
            val consumer = ResultConsumer(state)
            setContent(consumer, content)
        }

        fun setContent(
            state: ResultStore,
            saveableStateRegistry: SaveableStateRegistry,
            content: @Composable () -> Unit,
        ) {
            val consumer = ResultConsumer(state)
            composition.setContent {
                CompositionLocalProvider(
                    LocalResultConsumer provides consumer,
                    LocalSaveableStateRegistry provides saveableStateRegistry,
                ) {
                    content()
                }
            }
        }

        fun setContent(consumer: ResultConsumer, content: @Composable () -> Unit) {
            composition.setContent {
                CompositionLocalProvider(LocalResultConsumer provides consumer) {
                    content()
                }
            }
        }

        fun settle() {
            repeat(3) { frame ->
                recomposeWithoutRunningEffects(frame)
                scope.runCurrent()
            }
        }

        fun recomposeWithoutRunningEffects() {
            repeat(2) { frame -> recomposeWithoutRunningEffects(frame) }
        }

        private fun recomposeWithoutRunningEffects(frame: Int) {
            Snapshot.sendApplyNotifications()
            frameClock.sendFrame((scope.testScheduler.currentTime + frame) * 1_000_000)
        }
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(index: Int, instance: Unit): Unit = Unit

        override fun insertTopDown(index: Int, instance: Unit): Unit = Unit

        override fun move(from: Int, to: Int, count: Int): Unit = Unit

        override fun onClear(): Unit = Unit

        override fun remove(index: Int, count: Int): Unit = Unit
    }

    private class RecordingCallback(
        private val destination: MutableList<String>,
    ) : (String) -> Unit {
        override fun invoke(value: String) {
            destination += value
        }
    }

}
