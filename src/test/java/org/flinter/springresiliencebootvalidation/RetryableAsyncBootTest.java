package org.flinter.springresiliencebootvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootTest(classes = RetryableAsyncBootTest.Config.class)
class RetryableAsyncBootTest {

    @Autowired
    AsyncBean proxy;

    @Test
    void retryRunsInsideAsyncThread() {
        AsyncBean target = (AsyncBean) AopProxyUtils.getSingletonTarget(proxy);

        CompletableFuture<Void> future = proxy.retryOperation();

        assertThatExceptionOfType(CompletionException.class)
                .isThrownBy(future::join)
                .withCauseInstanceOf(IllegalStateException.class);

        // Async is OUTER, Retry is INNER: 3 total invocations (1 initial + 2 retries)
        assertThat(target.counter).hasValue(3);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableAsync
    @EnableResilientMethods
    static class Config {

        @Bean
        AsyncBean asyncBean() {
            return new AsyncBean();
        }
    }

    static class AsyncBean {

        AtomicInteger counter = new AtomicInteger();

        @Async
        @Retryable(maxRetries = 2, delay = 10)
        public CompletableFuture<Void> retryOperation() {
            throw new IllegalStateException(Integer.toString(counter.incrementAndGet()));
        }
    }
}
