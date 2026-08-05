package com.guentours.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.security.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        filter = new RateLimitFilter(rateLimiterService, properties, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void skipsNonApiPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiterService);
    }

    @Test
    void skipsEverythingWhenDisabled() throws Exception {
        properties.setEnabled(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimiterService);
    }

    @Test
    void classifiesLoginAndRegisterAsAuthCategory() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(rateLimiterService.tryConsume("203.0.113.5", RateLimitCategory.AUTH)).thenReturn(RateLimitResult.allow());

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).tryConsume("203.0.113.5", RateLimitCategory.AUTH);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void classifiesCheckoutAsBookingCategory() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/bookings/checkout");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(rateLimiterService.tryConsume("203.0.113.5", RateLimitCategory.BOOKING)).thenReturn(RateLimitResult.allow());

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).tryConsume("203.0.113.5", RateLimitCategory.BOOKING);
    }

    @Test
    void classifiesEverythingElseAsGeneralCategory() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/search/flights");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(rateLimiterService.tryConsume("203.0.113.5", RateLimitCategory.GENERAL)).thenReturn(RateLimitResult.allow());

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).tryConsume("203.0.113.5", RateLimitCategory.GENERAL);
    }

    @Test
    void trustsOnlyTheLastHopOfXForwardedForAsTheClientKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        // A malicious client could prepend a spoofed IP; only Nginx's own appended (last) hop is trusted.
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 203.0.113.5");
        when(rateLimiterService.tryConsume("203.0.113.5", RateLimitCategory.AUTH)).thenReturn(RateLimitResult.allow());

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).tryConsume("203.0.113.5", RateLimitCategory.AUTH);
        verify(request, never()).getRemoteAddr();
    }

    @Test
    void rejectsWithTooManyRequestsAndRetryAfterWhenDenied() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(rateLimiterService.tryConsume(any(), any())).thenReturn(RateLimitResult.deny(7));
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "7");
        verifyNoInteractions(filterChain);
        assertThat(body.toString()).contains("Trop de requêtes");
    }
}
