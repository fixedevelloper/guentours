package com.guentours.provider.sabre;

import com.guentours.provider.ProviderProperties;
import com.guentours.shared.exception.ProviderException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pins Sabre's OAuth exchange to its verified v3 contract: {@code POST /v3/auth/token} with a plain
 * HTTP Basic {@code base64(clientId:clientSecret)} header and a {@code password} grant body. Also
 * checks that the token is cached and that a rejected exchange surfaces Sabre's real reason (an
 * empty-body 401 hints at a bad key/secret or CERT-vs-production host mismatch) rather than a bare
 * "401 [no body]".
 */
class SabreTokenProviderTest {

    private static final String HOST = "https://api.cert.platform.sabre.com";
    private static final String SUCCESS_BODY =
            "{\"access_token\":\"tok-123\",\"token_type\":\"bearer\",\"expires_in\":604800}";

    private ProviderProperties.Vendor vendor() {
        ProviderProperties.Vendor v = new ProviderProperties.Vendor();
        v.setBaseUrl(HOST);
        v.setApiKey("my-key");
        v.setApiSecret("my-secret");
        v.setUsername("agent-USER");
        v.setPassword("agent-pass");
        return v;
    }

    @Test
    void sendsPlainBasicHeaderAndPasswordGrantThenReturnsTheToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl(HOST);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String expectedBasic = "Basic " + Base64.getEncoder()
                .encodeToString("my-key:my-secret".getBytes(StandardCharsets.UTF_8));

        server.expect(once(), requestTo(HOST + "/v3/auth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", expectedBasic))
                .andExpect(content().string(containsString("grant_type=password")))
                .andExpect(content().string(containsString("username=agent-USER")))
                .andExpect(content().string(containsString("password=agent-pass")))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        SabreTokenProvider provider = new SabreTokenProvider(builder.build(), vendor());

        assertThat(provider.getAccessToken()).isEqualTo("tok-123");
        server.verify();
    }

    @Test
    void cachesTheTokenAcrossCallsInsteadOfReAuthenticatingEveryTime() {
        RestClient.Builder builder = RestClient.builder().baseUrl(HOST);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // Expected exactly once: a second getAccessToken() must be served from the cache.
        server.expect(once(), requestTo(HOST + "/v3/auth/token"))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        SabreTokenProvider provider = new SabreTokenProvider(builder.build(), vendor());

        assertThat(provider.getAccessToken()).isEqualTo("tok-123");
        assertThat(provider.getAccessToken()).isEqualTo("tok-123");
        server.verify();
    }

    @Test
    void surfacesTheHostAndAHintWhenSabreRejectsWithAnEmptyBody() {
        RestClient.Builder builder = RestClient.builder().baseUrl(HOST);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(HOST + "/v3/auth/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        SabreTokenProvider provider = new SabreTokenProvider(builder.build(), vendor());

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("401")
                .hasMessageContaining(HOST)
                .hasMessageContaining("empty body");
    }

    @Test
    void surfacesSabresErrorBodyWhenItRejectsWithADescription() {
        RestClient.Builder builder = RestClient.builder().baseUrl(HOST);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(HOST + "/v3/auth/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"invalid_client\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        SabreTokenProvider provider = new SabreTokenProvider(builder.build(), vendor());

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("invalid_client");
    }
}
