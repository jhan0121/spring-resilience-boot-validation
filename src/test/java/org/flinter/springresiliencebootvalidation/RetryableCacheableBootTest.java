package org.flinter.springresiliencebootvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;

@SpringBootTest(classes = RetryableCacheableBootTest.Config.class)
class RetryableCacheableBootTest {

    @Autowired
    CacheableBean proxy;

    @Autowired
    CountingCacheManager cacheManager;

    @Test
    void retryOnCacheMissThenCacheOnSuccess() {
        CacheableBean target = (CacheableBean) AopProxyUtils.getSingletonTarget(proxy);

        String result = proxy.getValue();

        assertThat(result).isEqualTo("result");
        assertThat(target.counter).hasValue(2);
        // Retry is OUTER, so each attempt passes through the cache interceptor: get called 2 times.
        // If Cache were OUTER instead, get would only be called once.
        assertThat(cacheManager.gets).isEqualTo(2);

        proxy.getValue();

        assertThat(target.counter).hasValue(2); // Cache hit, target not invoked
        assertThat(cacheManager.gets).isEqualTo(3); // One more get for the cache hit
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

        @Bean
        CountingCacheManager cacheManager() {
            return new CountingCacheManager();
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

    static class CountingCacheManager implements CacheManager {

        int gets = 0;
        private final ConcurrentMapCacheManager delegate = new ConcurrentMapCacheManager();

        @Override
        public Cache getCache(String name) {
            Cache c = delegate.getCache(name);
            return c == null ? null : new CountingCache(c);
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }

        class CountingCache implements Cache {

            private final Cache delegate;

            CountingCache(Cache delegate) {
                this.delegate = delegate;
            }

            @Override
            public ValueWrapper get(Object key) {
                gets++;
                return delegate.get(key);
            }

            @Override
            public String getName() {
                return delegate.getName();
            }

            @Override
            public Object getNativeCache() {
                return delegate.getNativeCache();
            }

            @Override
            public <T> T get(Object key, @Nullable Class<T> type) {
                gets++;
                return delegate.get(key, type);
            }

            @Override
            public <T> T get(Object key, Callable<T> valueLoader) {
                gets++;
                return delegate.get(key, valueLoader);
            }

            @Override
            public void put(Object key, @Nullable Object value) {
                delegate.put(key, value);
            }

            @Override
            public void evict(Object key) {
                delegate.evict(key);
            }

            @Override
            public void clear() {
                delegate.clear();
            }
        }
    }
}
