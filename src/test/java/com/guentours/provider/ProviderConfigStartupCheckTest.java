package com.guentours.provider;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderConfigStartupCheckTest {

    @Test
    void reportsBlankAndNullRequiredSettingsSortedByName() {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("SABRE_API_SECRET", "");
        required.put("SABRE_API_KEY", "present");
        required.put("SABRE_PSEUDO_CITY_CODE", null);
        required.put("SABRE_USERNAME", "  ");

        assertThat(ProviderConfigStartupCheck.missingEnvVars(required))
                .containsExactly("SABRE_API_SECRET", "SABRE_PSEUDO_CITY_CODE", "SABRE_USERNAME");
    }

    @Test
    void reportsNothingWhenEverythingIsSet() {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("TRAVELPORT_API_KEY", "k");
        required.put("TRAVELPORT_API_SECRET", "s");

        assertThat(ProviderConfigStartupCheck.missingEnvVars(required)).isEmpty();
    }
}
