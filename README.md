# retry-kit

[![CI](https://github.com/philiprehberger/kt-retry-kit/actions/workflows/publish.yml/badge.svg)](https://github.com/philiprehberger/kt-retry-kit/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.philiprehberger/retry-kit)](https://central.sonatype.com/artifact/com.philiprehberger/retry-kit)
[![License](https://img.shields.io/github/license/philiprehberger/kt-retry-kit)](LICENSE)

Coroutine-native retry with configurable backoff strategies for Kotlin.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.philiprehberger:retry-kit:0.1.4")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.philiprehberger:retry-kit:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.philiprehberger</groupId>
    <artifactId>retry-kit</artifactId>
    <version>0.1.4</version>
</dependency>
```

## Usage

```kotlin
import com.philiprehberger.retrykit.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Simple retry with defaults (3 attempts, 100ms fixed backoff)
val result = retry {
    fetchFromApi()
}

// Retry with exponential backoff
val data = retry(
    maxAttempts = 5,
    backoff = BackoffStrategy.Exponential(100.milliseconds, max = 10.seconds),
) {
    httpClient.get("/data")
}
```

### RetryPolicy DSL

```kotlin
val policy = retryPolicy {
    maxAttempts(5)
    exponentialBackoff(100.milliseconds, max = 10.seconds)
    jitter(0.1)
    retryOn<IOException>()
    onRetry { attempt, ex -> logger.warn("Attempt $attempt failed: $ex") }
}

val result = policy.execute {
    riskyOperation()
}
```

### Backoff Strategies

```kotlin
// Fixed delay between retries
BackoffStrategy.Fixed(200.milliseconds)

// Exponential backoff (100ms, 200ms, 400ms, ...)
BackoffStrategy.Exponential(base = 100.milliseconds, max = 30.seconds)

// Exponential with jitter to avoid thundering herd
BackoffStrategy.ExponentialWithJitter(base = 100.milliseconds, max = 30.seconds, jitterFactor = 0.1)
```

## API

| Class / Function | Description |
|------------------|-------------|
| `retry()` | Suspend function that executes a block with retry logic |
| `retryPolicy { }` | DSL builder for creating reusable retry policies |
| `RetryPolicy` | Configurable retry policy with `execute()` method |
| `BackoffStrategy.Fixed` | Constant delay between retries |
| `BackoffStrategy.Exponential` | Exponentially increasing delay with a cap |
| `BackoffStrategy.ExponentialWithJitter` | Exponential backoff with random jitter |

## Development

```bash
./gradlew test       # Run tests
./gradlew check      # Run all checks
./gradlew build      # Build JAR
```

## License

MIT
