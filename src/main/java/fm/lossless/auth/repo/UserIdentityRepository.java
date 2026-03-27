package fm.lossless.auth.repo;

import fm.lossless.auth.domain.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {
    Optional<UserIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
