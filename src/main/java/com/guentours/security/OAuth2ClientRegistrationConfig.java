package com.guentours.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Google/Facebook OAuth2 client registrations by hand instead of through
 * spring.security.oauth2.client.registration.* properties - see application.yml for why (Boot's
 * own property-based config crashes at startup on a blank client id, even when nothing uses it).
 * Only ever registered (see {@link OAuth2ProviderConfiguredCondition}) once at least one
 * provider's credentials are actually set - until then this bean, and everything Boot would
 * otherwise auto-configure on top of it, simply don't exist.
 */
@Configuration
class OAuth2ClientRegistrationConfig {

    @Bean
    @Conditional(OAuth2ProviderConfiguredCondition.class)
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret,
            @Value("${FACEBOOK_CLIENT_ID:}") String facebookClientId,
            @Value("${FACEBOOK_CLIENT_SECRET:}") String facebookClientSecret) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (isConfigured(googleClientId, googleClientSecret)) {
            registrations.add(google(googleClientId, googleClientSecret));
        }
        if (isConfigured(facebookClientId, facebookClientSecret)) {
            registrations.add(facebook(facebookClientId, facebookClientSecret));
        }
        // Guaranteed non-empty: OAuth2ProviderConfiguredCondition already verified at least one
        // provider is configured before Spring even invokes this method.
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private boolean isConfigured(String clientId, String clientSecret) {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    private ClientRegistration google(String clientId, String clientSecret) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .clientName("Google")
                .build();
    }

    private ClientRegistration facebook(String clientId, String clientSecret) {
        return ClientRegistration.withRegistrationId("facebook")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email", "public_profile")
                .authorizationUri("https://www.facebook.com/v19.0/dialog/oauth")
                .tokenUri("https://graph.facebook.com/v19.0/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
                .userNameAttributeName("id")
                .clientName("Facebook")
                .build();
    }
}
