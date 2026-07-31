package com.guentours.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Sends the browser back to the frontend's callback page with an error rather than a token,
 *  when the user denies consent or the provider rejects the exchange. */
@Component
class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final String frontendRedirectUri;

    OAuth2LoginFailureHandler(@Value("${app.oauth2.frontend-redirect-uri}") String frontendRedirectUri) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.sendRedirect(frontendRedirectUri + "?error=" +
                URLEncoder.encode("Connexion refusée ou annulée", StandardCharsets.UTF_8));
    }
}
