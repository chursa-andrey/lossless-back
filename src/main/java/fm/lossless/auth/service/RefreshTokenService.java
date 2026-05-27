package fm.lossless.auth.service;

import fm.lossless.auth.config.JwtProperties;
import fm.lossless.auth.domain.RefreshToken;
import fm.lossless.auth.exception.InvalidRefreshTokenException;
import fm.lossless.auth.repo.RefreshTokenRepository;
import fm.lossless.auth.security.JwtService;
import fm.lossless.auth.web.dto.TokenPairResponse;
import fm.lossless.users.domain.Role;
import fm.lossless.users.domain.User;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;

        if (jwtProperties.getRefreshTtlDays() <= 0) {
            throw new IllegalArgumentException("Refresh token TTL must be positive.");
        }
        this.refreshTokenTtl = Duration.ofDays(jwtProperties.getRefreshTtlDays());
    }

    @Transactional
    public TokenPairResponse issueForUser(User user) {
        Instant now = now();
        String rawToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.issue(
                user,
                hashRefreshToken(rawToken),
                now,
                now.plus(refreshTokenTtl)
        );
        refreshTokenRepository.save(refreshToken);

        var roles = user.getRoles().stream().map(Role::getCode).toList();
        String accessToken = jwtService.createAccessToken(user.getId(), roles);
        return new TokenPairResponse(accessToken, rawToken);
    }

    @Transactional
    public TokenPairResponse refresh(String rawRefreshToken) {
        Instant now = now();
        String tokenHash = hashRefreshToken(rawRefreshToken);

        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);

        if (!currentToken.isActive(now)) {
            throw invalidRefreshToken();
        }

        currentToken.revoke(now);
        try {
            // Flush immediately to detect concurrent reuse before issuing a new token.
            refreshTokenRepository.flush();
        } catch (OptimisticLockingFailureException ex) {
            throw invalidRefreshToken();
        }

        return issueForUser(currentToken.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hashRefreshToken(rawToken);
        Instant now = now();
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(now);
            }
        });
    }

    @Transactional
    public long cleanupExpired() {
        return deleteExpiredTokens();
    }

    @Scheduled(fixedDelayString = "${auth.jwt.refresh-cleanup-fixed-delay-ms:86400000}")
    @Transactional
    public void cleanupExpiredScheduled() {
        deleteExpiredTokens();
    }

    private long deleteExpiredTokens() {
        return refreshTokenRepository.deleteByExpiresAtBefore(now());
    }

    private InvalidRefreshTokenException invalidRefreshToken() {
        return new InvalidRefreshTokenException();
    }

    private String hashRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String generateRawToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
