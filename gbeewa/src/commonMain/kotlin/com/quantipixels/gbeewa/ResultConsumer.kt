package com.quantipixels.gbeewa

import androidx.compose.runtime.Stable

/**
 * Provides consume-only access to a [ResultStore].
 *
 * Consumption is independent of navigation direction. Any current or later Compose destination
 * can consume an equal key when it uses a consumer backed by the same store. When consumers
 * compete for one pending key, only the first successful removal receives the value.
 *
 * @param store the producer-owned transient store for pending results.
 */
@Stable
public class ResultConsumer public constructor(
    private val store: ResultStore,
) {
    /** Returns `true` when [key] has a pending result. */
    public operator fun contains(key: ResultKey<*>): Boolean = key in store

    /**
     * Removes and returns the pending result for [key].
     *
     * This function returns [PendingResult.Missing] when the key has no pending result. A present
     * nullable result returns [PendingResult.Value] whose value is `null`.
     *
     * Direct consumption is intended for event handlers, effects, and non-Compose code. Do not
     * call it during composition because removal changes observable Compose state.
     *
     * @param key the typed result lane to consume.
     * @return the removed value or [PendingResult.Missing].
     */
    public fun <T> consume(key: ResultKey<T>): PendingResult<T> = store.remove(key)

    /**
     * Removes and delivers the pending result for [key].
     *
     * This function does nothing when the key has no pending result. It removes a present result
     * before it invokes [onResult]. A callback failure does not restore the result.
     *
     * Unsafe casts that bypass the typed-key contract are outside the portable contract. Runtimes
     * that enforce the erased callback cast fail visibly after removal.
     *
     * @param key the typed result lane to consume.
     * @param onResult the non-suspending callback that receives a present result.
     */
    public fun <T> consume(key: ResultKey<T>, onResult: (T) -> Unit) {
        when (val result = consume(key)) {
            PendingResult.Missing -> Unit
            is PendingResult.Value -> onResult(result.value)
        }
    }
}
