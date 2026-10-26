package com.quantipixels.gbeewa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The composition-local consumer used by [ResultEffect].
 *
 * Applications must provide a [ResultConsumer] with `CompositionLocalProvider`. Reading this
 * local without a provider fails immediately with a clear configuration error.
 */
public val LocalResultConsumer: ProvidableCompositionLocal<ResultConsumer> =
    staticCompositionLocalOf {
        error(
            "LocalResultConsumer is not provided. " +
                "Install a ResultConsumer with CompositionLocalProvider.",
        )
    }

/**
 * Delivers one pending result for [key] to the latest [onResult] callback.
 *
 * The effect consumes a result that exists before this function enters composition or that is
 * published while it remains mounted. Consumption removes the result before the callback runs,
 * so recomposition does not replay it and competing effects deliver it at most once. Leaving
 * composition does not discard an unconsumed result.
 *
 * Delivery is independent of navigation direction. The effect can receive on the publishing
 * screen or on a screen that becomes active after a pop, push, replacement, or other navigation
 * change. It must use a consumer backed by the publishing [ResultStore] and an equal [key].
 *
 * The callback is non-suspending and must finish quickly. Transfer longer work to state or launch
 * it in an application-owned scope with the required lifecycle and error policy.
 *
 * Do not publish another value for [key] from inside [onResult]. The current keyed effect has
 * completed, so that value can remain pending until this effect leaves and re-enters composition.
 * This includes a payload equal to the consumed value; callback republishing has no automatic
 * redelivery or retry guarantee.
 *
 * @param key the typed result lane to observe.
 * @param onResult the latest callback that receives a pending result.
 */
@Composable
public fun <T> ResultEffect(
    key: ResultKey<T>,
    onResult: (T) -> Unit,
) {
    val consumer = LocalResultConsumer.current
    val currentOnResult by rememberUpdatedState(onResult)

    if (key in consumer) {
        LaunchedEffect(consumer, key) {
            consumer.consume(key, currentOnResult)
        }
    }
}
