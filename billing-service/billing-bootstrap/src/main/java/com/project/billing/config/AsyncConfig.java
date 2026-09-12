package com.project.billing.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables {@code @Async} and supplies the executor backing it.
 *
 * Used for genuinely fire-and-forget side effects that must not sit on the
 * request (or transaction-committing) thread — currently the after-commit bridge
 * that publishes usage-charge events to Kafka. The pool is bounded with a
 * caller-runs policy so a backlog applies back-pressure instead of growing
 * unboundedly.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("billing-async-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        // Back-pressure: when the queue is full, run on the caller thread rather
        // than dropping the side effect.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    /** Uncaught exceptions in void @Async methods are logged rather than swallowed. */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
