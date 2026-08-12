package com.quantipixels.gbeewa

/**
 * Describes whether a [ResultConsumer] removed a pending result.
 *
 * A nullable result is represented by [Value] with a `null` [Value.value]. It is distinct from
 * [Missing], which means that the key had no pending result.
 *
 * @param T the payload type declared by the consumed result key.
 */
public sealed interface PendingResult<out T> {
    /** No pending result existed for the requested key. */
    public data object Missing : PendingResult<Nothing>

    /** A pending [value] was removed for the requested key. */
    public data class Value<out T> public constructor(
        public val value: T,
    ) : PendingResult<T>
}
