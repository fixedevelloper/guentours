package com.guentours.security;

import com.guentours.user.domain.User;
import com.guentours.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService, AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerNewUser(request.email(), request.fullName(), request.phone(), request.password());
        AppUserPrincipal principal = new AppUserPrincipal(user);
        String token = jwtService.generateToken(principal, principal.getRole());
        return ResponseEntity.ok(AuthResponse.of(token, user.getEmail(), user.getFullName(), principal.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userService.getByEmail(request.email());
        AppUserPrincipal principal = new AppUserPrincipal(user);
        String token = jwtService.generateToken(principal, principal.getRole());
        return ResponseEntity.ok(AuthResponse.of(token, user.getEmail(), user.getFullName(), principal.getRole()));
    }
}
