package fm.lossless.auth.repo;

import fm.lossless.auth.domain.UserPasswordCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordCredentialRepository extends JpaRepository<UserPasswordCredential, Long> {
}
