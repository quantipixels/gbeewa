package com.quantipixels.gbeewa

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

/**
 * Stores the latest pending result for each [ResultKey].
 *
 * The owner publishes results through [put]. The store retains each value until a
 * [ResultConsumer] consumes it, a later value replaces it, or the owner discards this store.
 * This store lives in memory and is not serialized or restored. Restoring navigation does not
 * restore pending results. If a destination requires a transient handoff, the caller must choose
 * how to recover from a missing result before starting work that depends on it.
 *
 * Shared store ownership, equal keys, and consumption timing select the receiver; navigation
 * direction does not. Publish and consume on the UI or main context that drives Compose
 * consumption. This store does not switch contexts or synchronize concurrent access.
 */
@Stable
public class ResultStore public constructor() {
    private val pendingResults = mutableStateMapOf<ResultKey<*>, Any?>()

    /**
     * Stores [value] for [key], replacing an earlier pending value for the same key.
     *
     * The first eligible consumer backed by this store that consumes an equal key receives the
     * value. A currently mounted consumer can receive it before a later screen enters composition.
     *
     * @param key the typed result lane.
     * @param value the result to retain until consumption or replacement.
     */
    public fun <T> put(key: ResultKey<T>, value: T) {
        pendingResults[key] = value
    }

    /**
     * Returns `true` when [key] has a pending result.
     *
     * A pending nullable result with a `null` value is present and returns `true`.
     */
    public operator fun contains(key: ResultKey<*>): Boolean = key in pendingResults

    internal fun <T> remove(key: ResultKey<T>): PendingResult<T> {
        if (key !in pendingResults) return PendingResult.Missing

        @Suppress("UNCHECKED_CAST")
        val result = pendingResults.remove(key) as T
        return PendingResult.Value(result)
    }
}
