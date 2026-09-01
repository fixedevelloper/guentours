package com.guentours.search.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class SearchExecutorConfig {

    /**
     * Used to fan a single search request out to every provider concurrently - shared across all
     * four search services (flight/hotel/vehicle/property), each of which can occupy one thread
     * per enabled {@link com.guentours.provider.ProviderType} (5 today) for the duration of that
     * provider's call (up to its configured read timeout, tens of seconds for a slow provider).
     * A small fixed pool (previously 8) is a global bottleneck shared by every concurrent search
     * of every type - two or three simultaneous searches already exceed it, queuing later ones
     * behind whichever provider call is slowest. Virtual threads remove that ceiling: this is
     * pure I/O-bound waiting on external HTTP calls, exactly what they're for, and each one is
     * cheap enough that there's no real pool to size.
     */
    @Bean
    public ExecutorService providerSearchExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
