package org.flinter.springresiliencebootvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;

@SpringBootTest(classes = RetryableCacheableBootTest.Config.class)
class RetryableCacheableBootTest {

    @Autowired
    CacheableBean proxy;

    @Test
    void retryOnCacheMissThenCacheOnSuccess() {
        CacheableBean target = (CacheableBean) AopProxyUtils.getSingletonTarget(proxy);

        String result = proxy.getValue();

        assertThat(result).isEqualTo("result");
        assertThat(target.counter).hasValue(2);

        proxy.getValue();

        assertThat(target.counter).hasValue(2);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableCaching
    @EnableResilientMethods
    static class Config {

        @Bean
        CacheableBean cacheableBean() {
            return new CacheableBean();
        }
    }

    static class CacheableBean {

        AtomicInteger counter = new AtomicInteger();

        @Cacheable("validation")
        @Retryable(maxRetries = 2, delay = 10)
        public String getValue() {
            if (counter.incrementAndGet() < 2) {
                throw new IllegalStateException();
            }
            return "result";
        }
    }
}
