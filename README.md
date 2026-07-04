# spring-resilience-boot-validation

A validation project
for [spring-projects/spring-framework#35584](https://github.com/spring-projects/spring-framework/issues/35584),
confirming that `@Retryable` composes with other proxy-based features (`@Transactional`, `@Cacheable`, `@Async`)
the same way under Spring Boot auto-configuration as it does with raw Spring Framework configuration.

## Background

The issue asks for `@Retryable`'s advice-chain behavior to be verified under Spring Boot auto-configuration, not
just hand-configured `AnnotationConfigApplicationContext` setups, since auto-configuration can register advisors
differently. This project runs the same three scenarios under `@SpringBootTest` across every Spring Boot 4.x
release.

## Test Scenarios

| Test class                       | Combination                     |
|----------------------------------|---------------------------------|
| `RetryableTransactionalBootTest` | `@Retryable` + `@Transactional` |
| `RetryableCacheableBootTest`     | `@Retryable` + `@Cacheable`     |
| `RetryableAsyncBootTest`         | `@Retryable` + `@Async`         |

## Running the tests

```bash
./gradlew test
```

The Spring Boot version under test is set in `gradle.properties` (`bootVersion`). Uncomment a different version
there to validate against another Spring Boot 4.x release.
