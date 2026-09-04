package com.guentours.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Verifies a Google ID token / Facebook access token obtained natively by the mobile app
 * (google_sign_in / flutter_facebook_auth - there is no in-app browser step to trust here, unlike
 * the web {@code oauth2Login()} flow in {@link com.guentours.security.SecurityConfig}, so the raw
 * token handed to {@code AuthController}'s mobile endpoints must be checked against the provider
 * directly before any account is resolved/created from it).
 *
 * <p>Deliberately a plain outbound HTTPS call to each provider's own verification endpoint rather
 * than a vendor SDK (e.g. {@code google-api-client}'s {@code GoogleIdTokenVerifier}) - same
 * lightweight-HTTP-client convention already used for every provider integration in this codebase
 * (see {@code TravelTerminusClient} and friends), and both endpoints are the providers' own
 * documented way to verify a token server-side.
 *
 * <p>Public (unlike most classes in this module): its only caller, {@code
 * security.web.AuthController}, lives in a sub-package - same visibility need as e.g. {@code
 * BookingService} in the booking module.
 */
@Component
public class SocialTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(SocialTokenVerifier.class);

    private final RestClient restClient;
    private final String googleMobileClientId;
    private final String facebookAppId;
    private final String facebookAppSecret;

    public SocialTokenVerifier(RestClient.Builder restClientBuilder,
                        @Value("${app.oauth2.google-mobile-client-id:}") String googleMobileClientId,
                        // Same Facebook App - unlike Google, Facebook doesn't issue a separate
                        // client id per platform, just additional "Platform" entries (Android/iOS)
                        // configured within this one app in the Facebook Developer Console. Reuses
                        // the same credentials the web oauth2Login() registration already has
                        // (see OAuth2ClientRegistrationConfig), rather than a redundant new var.
                        @Value("${FACEBOOK_CLIENT_ID:}") String facebookAppId,
                        @Value("${FACEBOOK_CLIENT_SECRET:}") String facebookAppSecret) {
        this.restClient = restClientBuilder.build();
        this.googleMobileClientId = googleMobileClientId;
        this.facebookAppId = facebookAppId;
        this.facebookAppSecret = facebookAppSecret;
    }

    public record VerifiedSocialProfile(String email, String name) {
    }

    /** Google's tokeninfo endpoint both verifies the signature/expiry and returns the claims - one
     *  round trip covers verification and profile extraction. {@code aud} must match the mobile
     *  app's own Google OAuth client id (a different client id than the web one already configured
     *  for {@code oauth2Login()} - Google issues a distinct client per platform), or a token minted
     *  for some other application would be accepted here. */
    public VerifiedSocialProfile verifyGoogleIdToken(String idToken) {
        Map<String, Object> claims;
        try {
            claims = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            log.warn("Google ID token verification failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid Google ID token");
        }
        if (claims == null) {
            throw new BadCredentialsException("Invalid Google ID token");
        }

        String audience = String.valueOf(claims.get("aud"));
        if (googleMobileClientId.isBlank() || !googleMobileClientId.equals(audience)) {
            log.warn("Google ID token audience mismatch (expected configured mobile client id)");
            throw new BadCredentialsException("Invalid Google ID token");
        }

        String email = (String) claims.get("email");
        if (email == null || email.isBlank() || !"true".equals(String.valueOf(claims.get("email_verified")))) {
            throw new BadCredentialsException("Google account has no verified email");
        }
        String name = (String) claims.getOrDefault("name", email);
        return new VerifiedSocialProfile(email, name);
    }

    /** {@code /debug_token} confirms this access token was actually issued for *this* Facebook
     *  app (via an app access token, {@code app_id|app_secret}) before trusting it - without this,
     *  a valid access token from an unrelated Facebook app would otherwise be accepted just as
     *  readily, since a plain {@code /me} call alone doesn't check who the token was issued to. */
    private void verifyFacebookAppId(String accessToken) {
        if (facebookAppId.isBlank() || facebookAppSecret.isBlank()) {
            throw new BadCredentialsException("Facebook login is not configured");
        }
        Map<String, Object> debug;
        try {
            debug = restClient.get()
                    .uri("https://graph.facebook.com/debug_token?input_token={token}&access_token={appToken}",
                            accessToken, facebookAppId + "|" + facebookAppSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            log.warn("Facebook access token debug check failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid Facebook access token");
        }
        Object data = debug == null ? null : debug.get("data");
        if (!(data instanceof Map<?, ?> debugData)
                || !Boolean.TRUE.equals(debugData.get("is_valid"))
                || !facebookAppId.equals(String.valueOf(debugData.get("app_id")))) {
            log.warn("Facebook access token failed app-id/validity check");
            throw new BadCredentialsException("Invalid Facebook access token");
        }
    }

    /** One Graph API call both verifies the access token (an invalid/expired one 4xxs) and returns
     *  the profile fields needed to resolve/create the local account. */
    public VerifiedSocialProfile verifyFacebookAccessToken(String accessToken) {
        verifyFacebookAppId(accessToken);

        Map<String, Object> profile;
        try {
            profile = restClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,name,email&access_token={token}", accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            log.warn("Facebook access token verification failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid Facebook access token");
        }
        if (profile == null) {
            throw new BadCredentialsException("Invalid Facebook access token");
        }

        String email = (String) profile.get("email");
        if (email == null || email.isBlank()) {
            // Only granted if the user approved the "email" permission - no account to resolve without it.
            throw new BadCredentialsException("Facebook account did not grant an email address");
        }
        String name = (String) profile.getOrDefault("name", email);
        return new VerifiedSocialProfile(email, name);
    }
}
