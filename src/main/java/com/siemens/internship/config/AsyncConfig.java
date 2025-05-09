package com.siemens.internship.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * This class enables the asynchronous support for the applications by allowing Spring to look for @Async annotations.
 * When it meets a method annotated with this annotation, it runs it on a background thread that comes from the ThreadPoolTaskExecutor.
 * Therefore, the server can handle multiple requests more efficiently because it does not have to wait for the slow operations to finish.
 */

@Configuration
@EnableAsync
public class AsyncConfig {
    /**
     * This method defines the thread pool and its characteristics.
     * Core pool size - how many threads are kept alive, even if they’re idle.
     * Max pool size - in case the queue gets filled, the max number of threads that can be allowed. (usually 2 * corePoolSize)
     * Queue size - when all threads from the core pool are busy, requests enter the waiting queue
     * Thread Name prefix - all treas will be called Thread-1, Thread-2 etc. Helps with debugging when looking through the logs
     *
     * All these arguments can be adjusted based on the problem and the average number of requests made.
     * (these are tests values that I estimated should be ok)
     *
     */
    @Bean
    @Primary
    public Executor itemProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Thread-");
        executor.initialize();

        return executor;
    }
}
