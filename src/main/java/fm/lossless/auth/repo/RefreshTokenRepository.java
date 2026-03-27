package fm.lossless.auth.repo;

import fm.lossless.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBefore(Instant threshold);
}
