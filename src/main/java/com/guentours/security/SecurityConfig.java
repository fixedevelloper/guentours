package com.guentours.security;

import com.guentours.security.service.AppUserDetailsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/search/**",
            "/api/bookings/**",
            "/api/payments/**",
            "/api/tickets/**",
            "/api/geo/**",
            "/api/destinations/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    };

    /**
     * CSRF is only meaningful where a request's authorization comes from an ambient cookie the
     * browser attaches automatically (now true here, since the JWT lives in an HttpOnly cookie -
     * see AuthCookieService). Endpoints that are already {@code permitAll} don't need it: an
     * attacker's page can already call them directly without borrowing a victim's session, so a
     * CSRF token would add no protection - it would only get in the way of the public
     * login/register/guest-checkout/webhook flows below.
     */
    private static final String[] CSRF_IGNORED_ENDPOINTS = {
            "/api/auth/**",
            "/api/search/**",
            "/api/bookings/**",
            "/api/payments/**",
            "/api/tickets/**",
            "/api/geo/**",
            "/api/destinations/**",
            "/api/partners/register",
            "/api/newsletter/subscribe",
            "/oauth2/**",
            "/login/oauth2/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final List<String> allowedOrigins;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AppUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOrigins,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.allowedOrigins = allowedOrigins;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * A separate, higher-priority chain scoped to {@code /actuator/**} so it can enable HTTP Basic
     * without changing how the rest of the app reports unauthenticated access (the main chain
     * below has no {@code httpBasic()}/{@code formLogin()}, so it keeps responding 403 rather than
     * challenging with a {@code WWW-Authenticate: Basic} header - enabling Basic auth on a single
     * shared chain would have switched that to 401 app-wide). Basic auth here lets a scraper
     * (Prometheus, curl, ...) authenticate with an existing admin account's email/password,
     * checked through the same {@link #authenticationProvider()} as the JWT login flow - no
     * separate service-account store. health/info stay public for uptime checks and load
     * balancers; everything else under /actuator/** (metrics, prometheus, ...) is admin-only.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .authenticationProvider(authenticationProvider())
                .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CookieCsrfTokenRepository + CsrfTokenRequestAttributeHandler is the standard
                // Spring Security pattern for an SPA: the token is exposed as a readable
                // "XSRF-TOKEN" cookie (NOT HttpOnly, unlike the auth cookie) and the frontend's
                // axios client is configured to echo it back as the X-XSRF-TOKEN header on every
                // mutating request (see frontend/src/lib/api/client.ts). The attribute handler
                // (rather than the default deferred/Xor handler) is required here because that
                // default assumes a server-rendered form reading the token via a request
                // attribute, which a same-page SPA client never does.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(CSRF_IGNORED_ENDPOINTS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS, not IF_REQUIRED: with IF_REQUIRED, Spring Security's own
                // SessionManagementFilter silently creates a session and copies the
                // JwtAuthenticationFilter-derived Authentication into it on the very first
                // authenticated request (SessionFixationProtectionStrategy + an explicit
                // securityContextRepository.saveContext() call - see SessionManagementFilter,
                // triggered any time it finds an authenticated context that isn't already
                // session-backed, regardless of which filter put it there). That JSESSIONID then
                // keeps authenticating the caller even after /api/auth/logout clears the gt_auth
                // cookie, defeating logout entirely - confirmed by testing logout with curl before
                // this was caught. STATELESS swaps the shared SecurityContextRepository for a
                // request-attribute-only one, so nothing can leak into a session that way.
                // The OAuth2 login handshake below still works under STATELESS: it stores the
                // authorization request's "state" via HttpSessionOAuth2AuthorizationRequestRepository,
                // which calls request.getSession(true) directly and unconditionally - independent
                // of this policy, which only governs Spring Security's own automatic session use.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/partners/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/subscribe").permitAll()
                        // Liste complète des partenaires : réservée à l'admin (utilisée par AdminPartnersPage)
                        .requestMatchers(HttpMethod.GET, "/api/partners").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/partners/*/approve", "/api/partners/*/reject").hasRole("ADMIN")
                        // Le reste de /api/partners/** (profil, inventaire, réservations) : juste authentifié ici —
                        // c'est le @PreAuthorize au niveau des contrôleurs qui vérifie le rôle partenaire ET
                        // que #partnerId correspond bien au partenaire connecté (ou hasRole('ADMIN')).
                        .requestMatchers("/api/partners/**").authenticated()
                        .requestMatchers("/api/bookings/me").authenticated()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Only wired once at least one provider's credentials are set (see
        // OAuth2ClientRegistrationConfig) - .oauth2Login() would otherwise eagerly look up a
        // ClientRegistrationRepository bean that doesn't exist yet and fail to build the chain.
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler(oAuth2LoginFailureHandler));
        }

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new org.springframework.security.authentication.ProviderManager(authenticationProvider());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}