package com.guentours.security.web;

import com.guentours.security.*;
import com.guentours.security.service.JwtService;
import com.guentours.user.domain.User;
import com.guentours.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthCookieService authCookieService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager,
                          JwtService jwtService, AuthCookieService authCookieService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        User user = userService.registerNewUser(request.email(), request.fullName(), request.phone(), request.password());
        AppUserPrincipal principal = new AppUserPrincipal(user);
        String token = jwtService.generateToken(principal, principal.getRole());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.buildAuthCookie(token).toString());
        return ResponseEntity.ok(AuthResponse.of(null, user.getEmail(), user.getFullName(), principal.getRole(), user.getPartnerId(), user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userService.getByEmail(request.email());
        AppUserPrincipal principal = new AppUserPrincipal(user);
        String token = jwtService.generateToken(principal, principal.getRole());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.buildAuthCookie(token).toString());
        return ResponseEntity.ok(AuthResponse.of(null, user.getEmail(), user.getFullName(), principal.getRole(), user.getPartnerId(), user.getId()));
    }

    /**
     * Forces the XSRF-TOKEN cookie to be issued. Spring's CsrfToken is a lazy supplier - the
     * CookieCsrfTokenRepository only actually generates and sets the cookie when something reads
     * {@link CsrfToken#getToken()}, which never happens on its own for a JSON-only API with no
     * server-rendered view. The frontend calls this once on app bootstrap so the cookie exists
     * before the first authenticated mutating request needs to echo it back as X-XSRF-TOKEN.
     * Public and GET, so it also works for a not-yet-logged-in visitor.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        return ResponseEntity.noContent().build();
    }

    /** Clears the auth cookie. Public - clearing a cookie the caller doesn't have is harmless. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieService.buildLogoutCookie().toString());
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the current profile from the HttpOnly auth cookie alone - used by the social login
     * callback page, which only receives that cookie from the backend redirect (not a full
     * {@link AuthResponse}) and needs the rest of the profile to populate the session client-side.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        String email = SecurityUtils.currentUserEmail();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User user = userService.getByEmail(email);
        return ResponseEntity.ok(AuthResponse.of(null, user.getEmail(), user.getFullName(),
                user.getRole().name(), user.getPartnerId(), user.getId()));
    }
}