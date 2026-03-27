package fm.lossless.auth.service;

import fm.lossless.auth.exception.EmailAlreadyInUseException;
import fm.lossless.auth.repo.UserPasswordCredentialRepository;
import fm.lossless.auth.web.dto.RegisterRequest;
import fm.lossless.users.domain.Role;
import fm.lossless.users.repo.RoleRepository;
import fm.lossless.users.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPasswordCredentialRepository userPasswordCredentialRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerMapsDatabaseUniqueViolationToEmailAlreadyInUse() {
        RegisterRequest request = new RegisterRequest("race@example.com", "Passw0rd!", "Race");
        Role userRole = mock(Role.class);

        when(roleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.findByEmail("race@example.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }
}
