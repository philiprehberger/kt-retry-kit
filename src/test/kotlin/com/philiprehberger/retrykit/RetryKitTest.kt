package com.philiprehberger.retrykit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RetryKitTest {

    @Test
    fun `successful on first try returns immediately`() = runTest {
        val result = retry { 42 }
        assertEquals(42, result)
    }

    @Test
    fun `success after retries`() = runTest {
        var attempts = 0
        val result = retry(
            maxAttempts = 3,
            backoff = BackoffStrategy.Fixed(1.milliseconds),
        ) {
            attempts++
            if (attempts < 3) throw RuntimeException("fail")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `all attempts exhausted throws last exception`() = runTest {
        val ex = assertFailsWith<RuntimeException> {
            retry(
                maxAttempts = 3,
                backoff = BackoffStrategy.Fixed(1.milliseconds),
            ) {
                throw RuntimeException("always fail")
            }
        }
        assertEquals("always fail", ex.message)
    }

    @Test
    fun `CancellationException is not retried`() = runTest {
        var attempts = 0
        assertFailsWith<CancellationException> {
            retry(maxAttempts = 5, backoff = BackoffStrategy.Fixed(1.milliseconds)) {
                attempts++
                throw CancellationException("cancelled")
            }
        }
        assertEquals(1, attempts)
    }

    @Test
    fun `exponential backoff calculates increasing delays`() {
        val strategy = BackoffStrategy.Exponential(
            base = 100.milliseconds,
            max = 10.seconds,
            multiplier = 2.0,
        )
        assertEquals(100.milliseconds, strategy.delayFor(0))
        assertEquals(200.milliseconds, strategy.delayFor(1))
        assertEquals(400.milliseconds, strategy.delayFor(2))
        assertEquals(800.milliseconds, strategy.delayFor(3))
    }

    @Test
    fun `exponential backoff respects max`() {
        val strategy = BackoffStrategy.Exponential(
            base = 1.seconds,
            max = 5.seconds,
            multiplier = 2.0,
        )
        assertEquals(1.seconds, strategy.delayFor(0))
        assertEquals(2.seconds, strategy.delayFor(1))
        assertEquals(4.seconds, strategy.delayFor(2))
        assertEquals(5.seconds, strategy.delayFor(3)) // capped at max
    }

    @Test
    fun `retryOn filtering only retries matching exceptions`() = runTest {
        var attempts = 0
        assertFailsWith<IllegalStateException> {
            retry(
                maxAttempts = 5,
                backoff = BackoffStrategy.Fixed(1.milliseconds),
                retryOn = { it is IOException },
            ) {
                attempts++
                if (attempts == 1) throw IOException("transient")
                throw IllegalStateException("not retryable")
            }
        }
        assertEquals(2, attempts)
    }

    @Test
    fun `onRetry callback is invoked`() = runTest {
        val retryAttempts = mutableListOf<Int>()
        assertFailsWith<RuntimeException> {
            retry(
                maxAttempts = 3,
                backoff = BackoffStrategy.Fixed(1.milliseconds),
                onRetry = { attempt, _ -> retryAttempts.add(attempt) },
            ) {
                throw RuntimeException("fail")
            }
        }
        assertEquals(listOf(1, 2), retryAttempts)
    }

    @Test
    fun `RetryPolicy DSL works`() = runTest {
        var attempts = 0
        val policy = retryPolicy {
            maxAttempts(3)
            fixedBackoff(1.milliseconds)
        }
        val result = policy.execute {
            attempts++
            if (attempts < 2) throw RuntimeException("fail")
            "success"
        }
        assertEquals("success", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `RetryPolicy with exponential backoff and retryOn`() = runTest {
        val policy = retryPolicy {
            maxAttempts(5)
            exponentialBackoff(1.milliseconds, max = 100.milliseconds)
            retryOn<IOException>()
            onRetry { _, _ -> }
        }
        var attempts = 0
        val result = policy.execute {
            attempts++
            if (attempts < 3) throw IOException("transient")
            "done"
        }
        assertEquals("done", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `fixed backoff returns constant delay`() {
        val strategy = BackoffStrategy.Fixed(50.milliseconds)
        assertEquals(50.milliseconds, strategy.delayFor(0))
        assertEquals(50.milliseconds, strategy.delayFor(5))
        assertEquals(50.milliseconds, strategy.delayFor(100))
    }

    @Test
    fun `exponential with jitter produces bounded delays`() {
        val strategy = BackoffStrategy.ExponentialWithJitter(
            base = 100.milliseconds,
            max = 10.seconds,
            jitterFactor = 0.1,
        )
        repeat(100) {
            val delay = strategy.delayFor(0)
            assertTrue(delay.inWholeMilliseconds in 90..110, "Delay $delay should be near 100ms")
        }
    }
}
