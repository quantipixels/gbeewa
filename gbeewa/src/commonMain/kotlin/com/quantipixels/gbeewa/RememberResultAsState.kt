package com.quantipixels.gbeewa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Remembers state that starts from [initialValue] and updates when [resultKey] receives a result.
 *
 * The initializer runs when this helper enters composition. It runs again when [resultKey] or any
 * item in [keys] changes. Ordinary recomposition keeps the current value. Returning to an earlier
 * key creates new initial state; this helper does not cache state per result key.
 *
 * This helper installs one [ResultEffect]. Do not mount another consumer for the same key when
 * deterministic ownership of the result is required.
 *
 * @param resultKey the typed result lane that updates the returned state.
 * @param keys additional values that reset the remembered initial state when they change.
 * @param initialValue creates the initial value for the current result key and extra keys.
 * @return read-only Compose state containing the initial or most recently delivered value.
 */
@Composable
public fun <T> rememberResultAsState(
    resultKey: ResultKey<T>,
    vararg keys: Any?,
    initialValue: () -> T,
): State<T> {
    val state = remember(resultKey, *keys) { mutableStateOf(initialValue()) }
    ResultEffect(resultKey) { state.value = it }
    return state
}

/**
 * Remembers saveable state that starts from [initialValue] and updates when [resultKey] receives a
 * result.
 *
 * This helper saves only the receiver's initial or most recently delivered value through the
 * current Compose saveable-state registry. It does not save [ResultStore] or pending results,
 * and restoration does not consume or replay a result.
 *
 * The initializer runs when this helper enters composition without a restored value. Live changes
 * to [resultKey] or any item in [inputs] reset the state. Compose restoration does not validate the
 * inputs that existed when it saved the value.
 *
 * The automatic saver supports values accepted by the current saveable-state registry, including
 * a nullable value held in Compose mutable state. Keep saved values small and destination-scoped.
 *
 * This helper installs one [ResultEffect]. Do not mount another consumer for the same key when
 * deterministic ownership of the result is required.
 *
 * @param resultKey the typed result lane that updates the returned state.
 * @param inputs additional values that reset the state when they change during live composition.
 * @param initialValue creates the initial value when no saved value can be restored.
 * @return read-only Compose state containing the initial, restored, or most recently delivered
 * value.
 */
@Composable
public fun <T> rememberSaveableResultAsState(
    resultKey: ResultKey<T>,
    vararg inputs: Any?,
    initialValue: () -> T,
): State<T> {
    val state = rememberSaveable(resultKey, *inputs) {
        mutableStateOf(initialValue())
    }
    ResultEffect(resultKey) { state.value = it }
    return state
}

/**
 * Remembers saveable state that uses [saver] to save and restore its contained value.
 *
 * Use this overload when the result type is not supported by the current Compose saveable-state
 * registry. The saver applies to the value inside the returned state. It does not save
 * [ResultStore] or pending results.
 *
 * The initializer runs when this helper enters composition without a restored value. Live changes
 * to [resultKey] or any item in [inputs] reset the state. Compose restoration does not validate the
 * inputs that existed when it saved the value.
 *
 * This helper installs one [ResultEffect]. Do not mount another consumer for the same key when
 * deterministic ownership of the result is required.
 *
 * @param resultKey the typed result lane that updates the returned state.
 * @param inputs additional values that reset the state when they change during live composition.
 * @param saver saves and restores the value inside the returned Compose state.
 * @param initialValue creates the initial value when no saved value can be restored.
 * @return read-only Compose state containing the initial, restored, or most recently delivered
 * value.
 */
@Composable
public fun <T> rememberSaveableResultAsState(
    resultKey: ResultKey<T>,
    vararg inputs: Any?,
    saver: Saver<T, out Any>,
    initialValue: () -> T,
): State<T> {
    val state = rememberSaveable(resultKey, *inputs, stateSaver = saver) {
        mutableStateOf(initialValue())
    }
    ResultEffect(resultKey) { state.value = it }
    return state
}
