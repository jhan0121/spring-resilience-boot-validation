package org.flinter.springresiliencebootvalidation;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@SpringBootTest(classes = RetryableTransactionalBootTest.Config.class)
class RetryableTransactionalBootTest {

    @Autowired
    TransactionalBean proxy;

    @Autowired
    CountingTransactionManager txManager;

    @Test
    void retryWrapsTransaction() {
        TransactionalBean target = (TransactionalBean) AopProxyUtils.getSingletonTarget(proxy);

        proxy.retryOperation();

        assertThat(target.counter).hasValue(2);
        assertThat(txManager.rollbacks).isEqualTo(1);
        assertThat(txManager.commits).isEqualTo(1);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableResilientMethods
    static class Config {

        @Bean
        TransactionalBean transactionalBean() {
            return new TransactionalBean();
        }

        @Bean
        CountingTransactionManager transactionManager() {
            return new CountingTransactionManager();
        }
    }

    static class TransactionalBean {

        AtomicInteger counter = new AtomicInteger();

        @Transactional
        @Retryable(maxRetries = 2, delay = 10)
        public void retryOperation() {
            if (counter.incrementAndGet() < 2) {
                throw new IllegalStateException();
            }
        }
    }

    static class CountingTransactionManager extends AbstractPlatformTransactionManager {

        int begun, commits, rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object tx, TransactionDefinition def) {
            begun++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
