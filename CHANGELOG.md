# Changelog

All notable changes to this library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1] - 2026-03-18

- Fix CI badge and gradlew permissions

## [0.1.0] - 2026-03-17

### Added
- `retry()` suspend function with configurable max attempts, backoff, and filtering
- `retryPolicy { }` DSL for building reusable retry policies
- `BackoffStrategy.Fixed` for constant delay between retries
- `BackoffStrategy.Exponential` for exponentially increasing delays
- `BackoffStrategy.ExponentialWithJitter` for jittered exponential backoff
- Automatic `CancellationException` propagation (never retried)
- `onRetry` callback for logging and observability
- `retryOn` predicate for selective exception filtering
