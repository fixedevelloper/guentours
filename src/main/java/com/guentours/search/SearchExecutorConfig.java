package com.guentours.search;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class SearchExecutorConfig {

    /** Bounded pool used to fan a single search request out to every provider concurrently. */
    @Bean
    public ExecutorService providerSearchExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
