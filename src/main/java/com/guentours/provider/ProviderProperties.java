package com.guentours.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.providers")
public class ProviderProperties {

    private Vendor travelopro = new Vendor();
    private Vendor sabre = new Vendor();
    private Vendor travelport = new Vendor();

    public Vendor getTravelopro() {
        return travelopro;
    }

    public void setTravelopro(Vendor travelopro) {
        this.travelopro = travelopro;
    }

    public Vendor getSabre() {
        return sabre;
    }

    public void setSabre(Vendor sabre) {
        this.sabre = sabre;
    }

    public Vendor getTravelport() {
        return travelport;
    }

    public void setTravelport(Vendor travelport) {
        this.travelport = travelport;
    }

    public static class Vendor {
        private boolean enabled = true;
        /** When true (default until real sandbox credentials are supplied) the adapter returns simulated offers instead of calling the vendor. */
        private boolean mockMode = true;
        private String baseUrl = "";
        private String apiKey = "";
        private String apiSecret = "";
        /** Vendor account username, where the vendor's auth model is a plain user_id/password pair (e.g. Travelopro). */
        private String username = "";
        private String password = "";
        /** Agency Pseudo City Code required by Sabre's REST APIs to scope search/booking to an account. */
        private String pseudoCityCode = "";
        /** Travelopro-style "access" flag: "Test" against the sandbox, "Live" in production. */
        private String accessMode = "Test";
        /** Client IP required by some vendors (e.g. Travelopro) as an anti-fraud signal on every request. */
        private String clientIp = "127.0.0.1";
        /** Absolute OAuth token endpoint, where the vendor serves auth on a different host than its APIs (e.g. Travelport). */
        private String tokenUrl = "";
        /** Travelport access group (branch scope), sent as the XAUTH_TRAVELPORT_ACCESSGROUP header on every call. */
        private String accessGroup = "";
        private long timeoutMillis = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMockMode() {
            return mockMode;
        }

        public void setMockMode(boolean mockMode) {
            this.mockMode = mockMode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }

        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPseudoCityCode() {
            return pseudoCityCode;
        }

        public void setPseudoCityCode(String pseudoCityCode) {
            this.pseudoCityCode = pseudoCityCode;
        }

        public String getAccessMode() {
            return accessMode;
        }

        public void setAccessMode(String accessMode) {
            this.accessMode = accessMode;
        }

        public String getClientIp() {
            return clientIp;
        }

        public void setClientIp(String clientIp) {
            this.clientIp = clientIp;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getAccessGroup() {
            return accessGroup;
        }

        public void setAccessGroup(String accessGroup) {
            this.accessGroup = accessGroup;
        }
    }
}
