package com.guentours.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logs each travel provider's configuration state once the application is ready, so a
 * misconfiguration is obvious at a glance in the startup log. In particular, a provider left in
 * live mode ({@code mock-mode=false}) without the credentials it needs is flagged with a WARN -
 * otherwise the only symptom would be a runtime "failed, skipping this provider" per search.
 */
@Component
class ProviderConfigStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfigStartupCheck.class);

    private final ProviderProperties properties;

    ProviderConfigStartupCheck(ProviderProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    void logProviderConfig() {
        report("TRAVELOPRO", properties.getTravelopro(), requiredTravelopro(properties.getTravelopro()));
        report("SABRE", properties.getSabre(), requiredSabre(properties.getSabre()));
        report("TRAVELPORT", properties.getTravelport(), requiredTravelport(properties.getTravelport()));
    }

    private void report(String name, ProviderProperties.Vendor vendor, Map<String, String> requiredEnvVars) {
        if (!vendor.isEnabled()) {
            log.info("{} provider: disabled ({}_ENABLED=false).", name, name);
            return;
        }
        if (vendor.isMockMode()) {
            log.info("{} provider: mock-mode ON - returning simulated offers (set {}_MOCK_MODE=false for live calls).",
                    name, name);
            return;
        }
        List<String> missing = missingEnvVars(requiredEnvVars);
        if (missing.isEmpty()) {
            log.info("{} provider: LIVE (mock-mode=false), required credentials present.", name);
        } else {
            log.warn("{} provider is LIVE (mock-mode=false) but these required settings are blank: {}. "
                            + "Live API calls will fail and this provider will be skipped on every search. "
                            + "Set them, or set {}_MOCK_MODE=true to use simulated offers.",
                    name, missing, name);
        }
    }

    /** Names of the required settings whose value is null/blank, sorted, for a clear WARN message. */
    static List<String> missingEnvVars(Map<String, String> requiredEnvVars) {
        return requiredEnvVars.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isBlank())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private Map<String, String> requiredTravelopro(ProviderProperties.Vendor v) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("TRAVELOPRO_USERNAME", v.getUsername());
        required.put("TRAVELOPRO_PASSWORD", v.getPassword());
        return required;
    }

    private Map<String, String> requiredSabre(ProviderProperties.Vendor v) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("SABRE_API_KEY", v.getApiKey());
        required.put("SABRE_API_SECRET", v.getApiSecret());
        required.put("SABRE_USERNAME", v.getUsername());
        required.put("SABRE_PASSWORD", v.getPassword());
        required.put("SABRE_PSEUDO_CITY_CODE", v.getPseudoCityCode());
        return required;
    }

    private Map<String, String> requiredTravelport(ProviderProperties.Vendor v) {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("TRAVELPORT_API_KEY", v.getApiKey());
        required.put("TRAVELPORT_API_SECRET", v.getApiSecret());
        required.put("TRAVELPORT_USERNAME", v.getUsername());
        required.put("TRAVELPORT_PASSWORD", v.getPassword());
        required.put("TRAVELPORT_ACCESS_GROUP", v.getAccessGroup());
        required.put("TRAVELPORT_PCC", v.getPseudoCityCode());
        return required;
    }
}
