package fm.lossless.auth.service;

import fm.lossless.LosslessApplication;
import fm.lossless.auth.config.JwtProperties;
import fm.lossless.auth.exception.InvalidRefreshTokenException;
import fm.lossless.auth.repo.RefreshTokenRepository;
import fm.lossless.auth.web.dto.TokenPairResponse;
import fm.lossless.users.domain.Role;
import fm.lossless.users.domain.User;
import fm.lossless.users.repo.RoleRepository;
import fm.lossless.users.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {LosslessApplication.class, RefreshTokenServiceTest.FixedClockConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:refreshtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "auth.jwt.issuer=test-issuer",
        "auth.jwt.secret=test-test-test-test-test-test-test-test",
        "auth.jwt.access-ttl-minutes=15",
        "auth.jwt.refresh-ttl-days=30"
})
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void issueCreatesRefreshTokenHashAndAccessToken() {
        User user = createUser("issue@example.com");

        TokenPairResponse tokens = refreshTokenService.issueForUser(user);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(refreshTokenRepository.findByTokenHash(hash(tokens.refreshToken())))
                .isPresent()
                .get()
                .extracting(rt -> rt.getCreatedAt())
                .isEqualTo(clock.instant());
    }

    @Test
    void refreshIssuesNewAccessAndRefreshAndRevokesOldOne() {
        User user = createUser("refresh@example.com");
        TokenPairResponse initial = refreshTokenService.issueForUser(user);

        TokenPairResponse refreshed = refreshTokenService.refresh(initial.refreshToken());

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(initial.refreshToken());

        assertThatThrownBy(() -> refreshTokenService.refresh(initial.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsNotFoundToken() {
        assertThatThrownBy(() -> refreshTokenService.refresh("missing-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsExpiredToken() {
        User user = createUser("expired@example.com");
        TokenPairResponse initial = refreshTokenService.issueForUser(user);

        clock.plus(Duration.ofDays(jwtProperties.getRefreshTtlDays()).plusSeconds(1));

        assertThatThrownBy(() -> refreshTokenService.refresh(initial.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsRevokedToken() {
        User user = createUser("revoked@example.com");
        TokenPairResponse initial = refreshTokenService.issueForUser(user);

        refreshTokenService.revoke(initial.refreshToken());

        assertThatThrownBy(() -> refreshTokenService.refresh(initial.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void cleanupExpiredDeletesOldTokensOnly() {
        User user = createUser("cleanup@example.com");
        refreshTokenService.issueForUser(user);

        clock.plus(Duration.ofDays(jwtProperties.getRefreshTtlDays()).plusSeconds(1));
        long removed = refreshTokenService.cleanupExpired();

        assertThat(removed).isGreaterThan(0);
        assertThat(refreshTokenRepository.count()).isEqualTo(0);
    }

    @Test
    void concurrentReuseAllowsSingleSuccessfulRefresh() throws Exception {
        User user = createUser("concurrent@example.com");
        TokenPairResponse initial = refreshTokenService.issueForUser(user);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> refreshWithGate(initial.refreshToken(), startGate));
            Future<Boolean> second = executor.submit(() -> refreshWithGate(initial.refreshToken(), startGate));

            startGate.countDown();

            int successCount = 0;
            if (first.get()) {
                successCount++;
            }
            if (second.get()) {
                successCount++;
            }

            assertThat(successCount).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean refreshWithGate(String refreshToken, CountDownLatch gate) throws Exception {
        gate.await();
        try {
            refreshTokenService.refresh(refreshToken);
            return true;
        } catch (InvalidRefreshTokenException ex) {
            return false;
        }
    }

    private User createUser(String email) {
        Role defaultRole = roleRepository.findByCode("USER").orElseThrow();
        User user = User.create(email, "User");
        user.addRole(defaultRole);
        return userRepository.save(user);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneOffset zone;

        MutableClock(Instant instant, ZoneOffset zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneOffset getZone() {
            return zone;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        void plus(Duration duration) {
            this.instant = this.instant.plus(duration);
        }
    }
}
