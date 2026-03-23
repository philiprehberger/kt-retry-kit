# Changelog

## 0.1.5 (2026-03-22)

- Fix README compliance (badge label, installation format, remove Groovy section), reformat build.gradle.kts, standardize CHANGELOG

## 0.1.4 (2026-03-20)

- Standardize README: fix title, badges, version sync, remove Requirements section

## 0.1.3 (2026-03-20)

- Add issueManagement to POM metadata

## 0.1.2 (2026-03-18)

- Upgrade to Kotlin 2.0.21 and Gradle 8.12
- Enable explicitApi() for stricter public API surface
- Add issueManagement to POM metadata

## 0.1.1 (2026-03-18)

- Fix CI badge and gradlew permissions

## 0.1.0 (2026-03-17)

- `retry()` suspend function with configurable max attempts, backoff, and filtering
- `retryPolicy { }` DSL for building reusable retry policies
- `BackoffStrategy.Fixed` for constant delay between retries
- `BackoffStrategy.Exponential` for exponentially increasing delays
- `BackoffStrategy.ExponentialWithJitter` for jittered exponential backoff
- Automatic `CancellationException` propagation (never retried)
- `onRetry` callback for logging and observability
- `retryOn` predicate for selective exception filtering
