package com.guentours.security;

import org.springframework.security.access.AccessDeniedException;
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

    /** Returns the authenticated user's id, or {@code null} for anonymous/guest requests. */
    public static String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    /** Returns the authenticated partner account's {@code partnerId}, or {@code null} otherwise. */
    public static String currentPartnerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal.getPartnerId();
        }
        return null;
    }

    /** True when the current request is authenticated with {@code ROLE_ADMIN}. */
    public static boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /**
     * Guards a partner-owned resource: throws unless the caller is an admin or the authenticated
     * partner account whose {@code partnerId} matches the resource's own owner. Deliberately not
     * expressed as a class-level {@code @PreAuthorize("#partnerId == ...")} on partner controllers,
     * because most of their nested endpoints (room type, fare, availability, image ids) don't carry
     * {@code partnerId} as a method parameter at all - referencing it in a SpEL expression there
     * would fail to resolve. Checking the resource's own stored partnerId here works everywhere.
     */
    public static void verifyOwnsPartner(String resourceOwnerPartnerId) {
        if (isAdmin()) {
            return;
        }
        String myPartnerId = currentPartnerId();
        if (myPartnerId == null || !myPartnerId.equals(resourceOwnerPartnerId)) {
            throw new AccessDeniedException("Ce compte n'est pas le partenaire propriétaire de cette ressource");
        }
    }
}
