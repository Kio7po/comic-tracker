package com.github.kio7po.comic_tracker.adapter.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

class RateLimiterExecutorTest {

    // A single permit that never refreshes within the test's lifetime, and never waits for one
    // either - the first call consumes it, any further call is rejected immediately.
    private static RateLimiterConfig exhaustedAfterOneCall() {
        return RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    @Test
    void execute_returnsTheCallResultWhenPermitted() {
        RateLimiterExecutor executor = new RateLimiterExecutor(RateLimiter.of("single", RateLimiterConfig.ofDefaults()));

        assertThat(executor.execute(() -> "berserk")).isEqualTo("berserk");
    }

    @Test
    void execute_rejectsSubsequentCallsOnceTheSoleLimiterIsExhausted() {
        RateLimiter limiter = RateLimiter.of("solo", exhaustedAfterOneCall());
        RateLimiterExecutor executor = new RateLimiterExecutor(limiter);

        executor.execute(() -> "ok");

        assertThatThrownBy(() -> executor.execute(() -> "ok"))
                .isInstanceOf(RequestNotPermitted.class)
                .extracting(exception -> ((RequestNotPermitted) exception).getCausingRateLimiterName())
                .isEqualTo("solo");
    }

    @Test
    void execute_rejectsWhenTheOuterLimiterIsExhausted() {
        RateLimiter outer = RateLimiter.of("outer", exhaustedAfterOneCall());
        RateLimiter inner = RateLimiter.of("inner", RateLimiterConfig.ofDefaults());
        RateLimiterExecutor executor = new RateLimiterExecutor(outer, inner);

        executor.execute(() -> "ok");

        assertThatThrownBy(() -> executor.execute(() -> "ok"))
                .isInstanceOf(RequestNotPermitted.class)
                .extracting(exception -> ((RequestNotPermitted) exception).getCausingRateLimiterName())
                .isEqualTo("outer");
    }

    @Test
    void execute_rejectsWhenTheInnerLimiterIsExhausted() {
        RateLimiter outer = RateLimiter.of("outer", RateLimiterConfig.ofDefaults());
        RateLimiter inner = RateLimiter.of("inner", exhaustedAfterOneCall());
        RateLimiterExecutor executor = new RateLimiterExecutor(outer, inner);

        executor.execute(() -> "ok");

        assertThatThrownBy(() -> executor.execute(() -> "ok"))
                .isInstanceOf(RequestNotPermitted.class)
                .extracting(exception -> ((RequestNotPermitted) exception).getCausingRateLimiterName())
                .isEqualTo("inner");
    }

}
