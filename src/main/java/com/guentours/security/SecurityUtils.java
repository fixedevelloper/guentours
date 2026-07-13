package com.guentours.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Returns the authenticated user's email, or {@code null} for anonymous/guest requests. */
    public static String currentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getUsername();
        }
        return null;
    }
}
