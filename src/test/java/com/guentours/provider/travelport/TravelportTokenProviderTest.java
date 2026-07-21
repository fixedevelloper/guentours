package com.guentours.provider.travelport;

import com.guentours.provider.ProviderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pins Travelport's OAuth exchange to its contract: {@code POST} to the (separate-host) token URL
 * with a {@code password} grant carrying {@code client_id}/{@code client_secret} in the form body
 * (not a Basic header), plus {@code scope=openid}. Also checks that a rejected exchange surfaces
 * Travelport's {@code invalid_client}/"Client authentication failed" body so the cause is visible.
 */
class TravelportTokenProviderTest {

    private static final String TOKEN_URL = "https://oauth.pp.travelport.com/oauth/oauth20/token";

    private ProviderProperties.Vendor vendor() {
        ProviderProperties.Vendor v = new ProviderProperties.Vendor();
        v.setTokenUrl(TOKEN_URL);
        v.setApiKey("tp-key");
        v.setApiSecret("tp-secret");
        v.setUsername("tp-user");
        v.setPassword("tp-pass");
        return v;
    }

    @Test
    void sendsClientCredentialsInThePasswordGrantBodyThenReturnsTheToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=password")))
                .andExpect(content().string(containsString("client_id=tp-key")))
                .andExpect(content().string(containsString("client_secret=tp-secret")))
                .andExpect(content().string(containsString("username=tp-user")))
                .andExpect(content().string(containsString("password=tp-pass")))
                .andExpect(content().string(containsString("scope=openid")))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tp-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));

        TravelportTokenProvider provider = new TravelportTokenProvider(builder, vendor());

        assertThat(provider.getAccessToken()).isEqualTo("tp-token");
        server.verify();
    }

    @Test
    void surfacesInvalidClientWhenTravelportRejectsTheClientCredentials() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\n  \"error\" : \"invalid_client\",\n  "
                                + "\"error_description\" : \"Client authentication failed\"\n}")
                        .contentType(MediaType.APPLICATION_JSON));

        TravelportTokenProvider provider = new TravelportTokenProvider(builder, vendor());

        assertThatThrownBy(provider::getAccessToken)
                .hasMessageContaining("invalid_client")
                .hasMessageContaining("Client authentication failed");
    }
}
