package fm.lossless.auth.web;

import fm.lossless.auth.service.AuthService;
import fm.lossless.auth.service.SocialAuthService;
import fm.lossless.auth.web.dto.AuthResponse;
import fm.lossless.auth.web.dto.LoginRequest;
import fm.lossless.auth.web.dto.RefreshRequest;
import fm.lossless.auth.web.dto.RegisterRequest;
import fm.lossless.auth.web.dto.SocialAuthRequest;
import fm.lossless.auth.web.dto.TokenPairResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    public AuthController(AuthService authService, SocialAuthService socialAuthService) {
        this.authService = authService;
        this.socialAuthService = socialAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/social/{provider}")
    public ResponseEntity<AuthResponse> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody SocialAuthRequest request
    ) {
        return ResponseEntity.ok(socialAuthService.loginWithProvider(provider, request.providerToken()));
    }
}
