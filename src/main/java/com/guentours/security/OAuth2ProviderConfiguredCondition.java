package com.guentours.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * True once at least one social login provider has both a client id and secret set. Guards the
 * {@code ClientRegistrationRepository} bean so it's skipped entirely (not just "empty") when
 * neither is configured - Spring Boot's own OAuth2 client autoconfiguration only activates once a
 * {@code ClientRegistrationRepository} bean actually exists, so nothing OAuth2-related runs at all
 * until this becomes true.
 */
class OAuth2ProviderConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        boolean google = hasText(env.getProperty("GOOGLE_CLIENT_ID")) && hasText(env.getProperty("GOOGLE_CLIENT_SECRET"));
        boolean facebook = hasText(env.getProperty("FACEBOOK_CLIENT_ID")) && hasText(env.getProperty("FACEBOOK_CLIENT_SECRET"));
        return google || facebook;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
