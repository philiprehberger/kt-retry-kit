package com.philiprehberger.retrykit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Executes [block] with retry logic, using the specified backoff strategy.
 *
 * If all attempts are exhausted, the last exception is thrown. [CancellationException] is
 * never retried and is always rethrown immediately.
 *
 * @param T The return type of the block.
 * @param maxAttempts Maximum number of attempts (including the initial call).
 * @param backoff The [BackoffStrategy] to use between retries.
 * @param retryOn Predicate to determine whether a given exception should be retried.
 * @param onRetry Callback invoked before each retry with the attempt number (1-based) and the exception.
 * @param block The suspending block to execute.
 * @return The result of the first successful execution of [block].
 * @throws Throwable The last exception if all attempts fail, or [CancellationException] immediately.
 */
suspend fun <T> retry(
    maxAttempts: Int = 3,
    backoff: BackoffStrategy = BackoffStrategy.Fixed(100.milliseconds),
    retryOn: (Throwable) -> Boolean = { true },
    onRetry: (Int, Throwable) -> Unit = { _, _ -> },
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    var lastException: Throwable? = null
    for (attempt in 1..maxAttempts) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            lastException = e
            if (attempt == maxAttempts || !retryOn(e)) {
                throw e
            }
            onRetry(attempt, e)
            delay(backoff.delayFor(attempt - 1))
        }
    }
    // Unreachable, but the compiler needs it
    throw lastException!!
}
