package com.philiprehberger.retrykit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A configurable retry policy built via a DSL.
 *
 * Use [retryPolicy] to create instances.
 *
 * ```kotlin
 * val policy = retryPolicy {
 *     maxAttempts(5)
 *     exponentialBackoff(100.milliseconds, max = 10.seconds)
 *     jitter(0.1)
 *     retryOn<IOException>()
 *     onRetry { attempt, ex -> println("Attempt $attempt failed: $ex") }
 * }
 * val result = policy.execute { riskyOperation() }
 * ```
 */
public class RetryPolicy internal constructor(
    private val maxAttempts: Int,
    private val backoff: BackoffStrategy,
    private val retryOnPredicates: List<(Throwable) -> Boolean>,
    private val onRetryCallback: (Int, Throwable) -> Unit,
) {

    /**
     * Executes [block] according to this policy.
     *
     * @param T The return type of the block.
     * @param block The suspending block to execute.
     * @return The result of the first successful execution.
     * @throws Throwable The last exception if all attempts fail.
     */
    public suspend fun <T> execute(block: suspend () -> T): T {
        return retry(
            maxAttempts = maxAttempts,
            backoff = backoff,
            retryOn = { ex -> retryOnPredicates.isEmpty() || retryOnPredicates.any { it(ex) } },
            onRetry = onRetryCallback,
            block = block,
        )
    }

    /**
     * Builder DSL for constructing a [RetryPolicy].
     */
    public class Builder {
        private var maxAttempts: Int = 3
        private var backoff: BackoffStrategy = BackoffStrategy.Fixed(100.milliseconds)
        private val retryOnPredicates = mutableListOf<(Throwable) -> Boolean>()
        private var onRetryCallback: (Int, Throwable) -> Unit = { _, _ -> }

        /**
         * Sets the maximum number of attempts (including the initial call).
         */
        public fun maxAttempts(value: Int) {
            require(value >= 1) { "maxAttempts must be at least 1" }
            this.maxAttempts = value
        }

        /**
         * Uses a fixed backoff delay between retries.
         */
        public fun fixedBackoff(delay: Duration) {
            this.backoff = BackoffStrategy.Fixed(delay)
        }

        /**
         * Uses exponential backoff between retries.
         *
         * @param base The initial delay.
         * @param max The maximum delay cap.
         * @param multiplier The growth factor per attempt.
         */
        public fun exponentialBackoff(base: Duration, max: Duration = 30.seconds, multiplier: Double = 2.0) {
            this.backoff = BackoffStrategy.Exponential(base, max, multiplier)
        }

        /**
         * Adds random jitter to the current backoff strategy.
         *
         * Only applies if the current backoff is [BackoffStrategy.Exponential].
         * Converts it to [BackoffStrategy.ExponentialWithJitter].
         *
         * @param factor The jitter fraction (e.g., 0.1 = +/-10%).
         */
        public fun jitter(factor: Double) {
            val current = backoff
            if (current is BackoffStrategy.Exponential) {
                this.backoff = BackoffStrategy.ExponentialWithJitter(current.base, current.max, factor)
            }
        }

        /**
         * Only retry on exceptions of the specified type.
         */
        public inline fun <reified T : Throwable> retryOn() {
            retryOn(T::class)
        }

        /**
         * Only retry on exceptions of the specified type.
         */
        public fun retryOn(exceptionClass: KClass<out Throwable>) {
            retryOnPredicates.add { exceptionClass.isInstance(it) }
        }

        /**
         * Registers a callback invoked before each retry.
         *
         * @param callback Receives the attempt number (1-based) and the exception.
         */
        public fun onRetry(callback: (Int, Throwable) -> Unit) {
            this.onRetryCallback = callback
        }

        internal fun build(): RetryPolicy = RetryPolicy(
            maxAttempts = maxAttempts,
            backoff = backoff,
            retryOnPredicates = retryOnPredicates.toList(),
            onRetryCallback = onRetryCallback,
        )
    }
}

/**
 * Creates a [RetryPolicy] using a builder DSL.
 *
 * ```kotlin
 * val policy = retryPolicy {
 *     maxAttempts(5)
 *     exponentialBackoff(100.milliseconds, max = 10.seconds)
 *     retryOn<IOException>()
 * }
 * ```
 *
 * @param block Configuration block for the policy builder.
 * @return A configured [RetryPolicy] instance.
 */
public fun retryPolicy(block: RetryPolicy.Builder.() -> Unit): RetryPolicy {
    return RetryPolicy.Builder().apply(block).build()
}
