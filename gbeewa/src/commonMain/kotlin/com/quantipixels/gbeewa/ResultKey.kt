package com.quantipixels.gbeewa

import androidx.compose.runtime.Stable

/**
 * Identifies one typed lane for a pending result.
 *
 * Key equality defines lane identity. A key can declare a nullable payload type, such as
 * `ResultKey<String?>`, when `null` is a valid result. The key does not identify a screen or a
 * navigation direction.
 *
 * @param T the payload type accepted by this key.
 */
@Stable
public interface ResultKey<T>
