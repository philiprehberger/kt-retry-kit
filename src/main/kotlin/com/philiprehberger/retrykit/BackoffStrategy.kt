package com.philiprehberger.retrykit

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Defines how delay between retry attempts is calculated.
 */
sealed interface BackoffStrategy {

    /**
     * Calculates the delay duration for the given attempt number.
     *
     * @param attempt The attempt number (0-based, where 0 is the delay before the first retry).
     * @return The duration to wait before the next attempt.
     */
    fun delayFor(attempt: Int): Duration

    /**
     * Fixed delay between each retry attempt.
     *
     * @property delay The constant delay duration.
     */
    data class Fixed(val delay: Duration) : BackoffStrategy {
        override fun delayFor(attempt: Int): Duration = delay
    }

    /**
     * Exponential backoff with a configurable base, maximum, and multiplier.
     *
     * The delay for attempt `n` is `min(base * multiplier^n, max)`.
     *
     * @property base The initial delay duration.
     * @property max The maximum delay duration.
     * @property multiplier The factor by which the delay increases each attempt.
     */
    data class Exponential(
        val base: Duration,
        val max: Duration,
        val multiplier: Double = 2.0,
    ) : BackoffStrategy {
        override fun delayFor(attempt: Int): Duration {
            val delayMs = base.inWholeMilliseconds * multiplier.pow(attempt)
            return min(delayMs.toLong(), max.inWholeMilliseconds).milliseconds
        }
    }

    /**
     * Exponential backoff with added random jitter to avoid thundering herd problems.
     *
     * The delay for attempt `n` is `min(base * multiplier^n, max) * (1 +/- jitterFactor)`.
     *
     * @property base The initial delay duration.
     * @property max The maximum delay duration.
     * @property jitterFactor The fraction of the delay to randomize (e.g., 0.1 means +/-10%).
     */
    data class ExponentialWithJitter(
        val base: Duration,
        val max: Duration,
        val jitterFactor: Double = 0.1,
    ) : BackoffStrategy {
        override fun delayFor(attempt: Int): Duration {
            val delayMs = base.inWholeMilliseconds * 2.0.pow(attempt)
            val cappedMs = min(delayMs.toLong(), max.inWholeMilliseconds)
            val jitterRange = (cappedMs * jitterFactor).toLong()
            val jitter = if (jitterRange > 0) Random.nextLong(-jitterRange, jitterRange + 1) else 0L
            return (cappedMs + jitter).coerceAtLeast(0).milliseconds
        }
    }
}
