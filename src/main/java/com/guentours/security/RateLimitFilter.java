package com.guentours.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.security.service.RateLimiterService;
import com.guentours.shared.web.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces {@link RateLimiterService} on every {@code /api/**} request, keyed by client IP -
 * registered as a plain servlet filter (see {@code RateLimitFilterConfig}) rather than wired into
 * {@code SecurityConfig}'s {@code HttpSecurity} chain, so it runs before Spring Security entirely
 * and protects login/register attempts even before authentication is evaluated. Non-API requests
 * (static assets, actuator) are never rate-limited here.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/auth/login";
    private static final String REGISTER_PREFIX = "/api/auth/register";
    private static final String CHECKOUT_PREFIX = "/api/bookings/checkout";
    private static final String FORGOT_PASSWORD_PREFIX = "/api/auth/forgot-password";
    private static final String RESET_PASSWORD_PREFIX = "/api/auth/reset-password";


    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterService rateLimiterService, RateLimitProperties properties,
                           ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        RateLimitCategory category = categoryFor(path);
        String clientKey = clientKey(request);
        RateLimitResult result = rateLimiterService.tryConsume(clientKey, category);

        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests", "Trop de requêtes, veuillez réessayer dans un instant."));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitCategory categoryFor(String path) {
        if (path.startsWith(AUTH_PREFIX) || path.startsWith(REGISTER_PREFIX)
                               || path.startsWith(FORGOT_PASSWORD_PREFIX) || path.startsWith(RESET_PASSWORD_PREFIX)) {
            return RateLimitCategory.AUTH;
        }
        if (path.startsWith(CHECKOUT_PREFIX)) {
            return RateLimitCategory.BOOKING;
        }
        return RateLimitCategory.GENERAL;
    }

    /**
     * The API is only reachable through the VPS's own Nginx (see docker-compose: {@code
     * 127.0.0.1:8080}), a single trusted proxy hop - so the LAST entry in X-Forwarded-For is the
     * one Nginx itself appended for the real connecting client ({@code proxy_add_x_forwarded_for}),
     * and is safe to trust. Any earlier entries could be spoofed by the client itself and must
     * not be used, or a spammer could rotate a fake first IP to dodge the limit entirely.
     */
    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
