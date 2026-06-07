package com.ziyara.backend.infrastructure.aop;

import com.ziyara.backend.application.annotation.RateLimit;
import com.ziyara.backend.application.exception.RateLimitedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class Bucket4jRateLimitAspectTest {

    @RateLimit(key = "test:endpoint", maxPerMinute = 3)
    private void annotated3() {}

    @RateLimit(key = "test:single", maxPerMinute = 1)
    private void annotated1() {}

    @Test
    void allowsRequestsUpToLimit() throws Throwable {
        Bucket4jRateLimitAspect aspect = new Bucket4jRateLimitAspect();
        ProceedingJoinPoint pjp = mockPjp();
        RateLimit limit = rateLimit("annotated3");

        assertThatCode(() -> {
            aspect.checkRateLimit(pjp, limit);
            aspect.checkRateLimit(pjp, limit);
            aspect.checkRateLimit(pjp, limit);
        }).doesNotThrowAnyException();

        verify(pjp, times(3)).proceed();
    }

    @Test
    void throwsRateLimitedAfterExceedingLimit() throws Throwable {
        Bucket4jRateLimitAspect aspect = new Bucket4jRateLimitAspect();
        ProceedingJoinPoint pjp = mockPjp();
        RateLimit limit = rateLimit("annotated3");

        aspect.checkRateLimit(pjp, limit);
        aspect.checkRateLimit(pjp, limit);
        aspect.checkRateLimit(pjp, limit);

        assertThatThrownBy(() -> aspect.checkRateLimit(pjp, limit))
                .isInstanceOf(RateLimitedException.class)
                .hasMessageContaining("Too many requests");
    }

    @Test
    void firstRequestAlwaysPasses() throws Throwable {
        Bucket4jRateLimitAspect aspect = new Bucket4jRateLimitAspect();
        ProceedingJoinPoint pjp = mockPjp();
        RateLimit limit = rateLimit("annotated1");

        assertThatCode(() -> aspect.checkRateLimit(pjp, limit)).doesNotThrowAnyException();
        verify(pjp).proceed();
    }

    @Test
    void secondRequestExceedsLimitOfOne() throws Throwable {
        Bucket4jRateLimitAspect aspect = new Bucket4jRateLimitAspect();
        ProceedingJoinPoint pjp = mockPjp();
        RateLimit limit = rateLimit("annotated1");

        aspect.checkRateLimit(pjp, limit);

        assertThatThrownBy(() -> aspect.checkRateLimit(pjp, limit))
                .isInstanceOf(RateLimitedException.class);
    }

    private static ProceedingJoinPoint mockPjp() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature sig = mock(Signature.class);
        when(sig.toShortString()).thenReturn("TestController.action()");
        when(pjp.getSignature()).thenReturn(sig);
        return pjp;
    }

    private RateLimit rateLimit(String methodName) throws Exception {
        return getClass().getDeclaredMethod(methodName).getAnnotation(RateLimit.class);
    }
}
