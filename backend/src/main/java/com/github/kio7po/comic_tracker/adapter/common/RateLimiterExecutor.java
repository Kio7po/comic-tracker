package com.github.kio7po.comic_tracker.adapter.common;

import java.util.List;
import java.util.function.Supplier;

import io.github.resilience4j.ratelimiter.RateLimiter;

/**
 * Runs a call through one or more RateLimiters, outermost first - the first limiter's permit is
 * consumed even when a later one then rejects the call, so pass the coarser/cheaper limiter first.
 */
public final class RateLimiterExecutor {

    private final List<RateLimiter> limiters;

    public RateLimiterExecutor(RateLimiter... limiters) {
        this.limiters = List.of(limiters);
    }

    public <T> T execute(Supplier<T> call) {
        Supplier<T> decorated = call;
        for (int i = limiters.size() - 1; i >= 0; i--) {
            decorated = RateLimiter.decorateSupplier(limiters.get(i), decorated);
        }
        return decorated.get();
    }

}
