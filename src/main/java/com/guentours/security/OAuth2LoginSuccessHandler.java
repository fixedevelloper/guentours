package com.guentours.security;

import com.guentours.user.domain.User;
import com.guentours.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * On successful Google/Facebook login, resolves (or auto-provisions) the local account by email
 * and redirects the browser back to the frontend with a freshly-issued JWT. The app is otherwise
 * entirely stateless/Bearer-token based, so the session this OAuth2 handshake used is never
 * relied on again past this point.
 */
@Component
class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final String frontendRedirectUri;

    OAuth2LoginSuccessHandler(UserService userService, JwtService jwtService,
                              @Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null) {
            // Facebook only returns this if the "email" permission was granted; Google always
            // includes it for the "email" scope. Either way, there's no account to resolve without it.
            response.sendRedirect(frontendRedirectUri + "?error=" + URLEncoder.encode(
                    "Aucune adresse email fournie par ce fournisseur", StandardCharsets.UTF_8));
            return;
        }

        User user = userService.findOrCreateForOAuth(email, name != null ? name : email);
        AppUserPrincipal principal = new AppUserPrincipal(user);
        String token = jwtService.generateToken(principal, principal.getRole());

        response.sendRedirect(frontendRedirectUri + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
    }
}
