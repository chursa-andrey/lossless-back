package fm.lossless.auth.service;

import fm.lossless.auth.exception.DefaultRoleNotConfiguredException;
import fm.lossless.auth.exception.EmailAlreadyInUseException;
import fm.lossless.auth.exception.InvalidCredentialsException;
import fm.lossless.auth.domain.UserPasswordCredential;
import fm.lossless.auth.repo.UserPasswordCredentialRepository;
import fm.lossless.auth.web.dto.AuthResponse;
import fm.lossless.auth.web.dto.LoginRequest;
import fm.lossless.auth.web.dto.RegisterRequest;
import fm.lossless.auth.web.dto.TokenPairResponse;
import fm.lossless.users.domain.Role;
import fm.lossless.users.domain.User;
import fm.lossless.users.repo.RoleRepository;
import fm.lossless.users.repo.UserRepository;
import fm.lossless.users.web.dto.UserDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserRepository userRepository;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            UserPasswordCredentialRepository userPasswordCredentialRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.userPasswordCredentialRepository = userPasswordCredentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = User.normalizeEmail(request.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyInUseException();
        }

        User user = User.create(normalizedEmail, request.displayName());
        user.addRole(loadDefaultUserRole());
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Handles race condition when two requests register the same email concurrently.
            throw new EmailAlreadyInUseException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        UserPasswordCredential credential = UserPasswordCredential.create(savedUser, passwordHash);
        userPasswordCredentialRepository.save(credential);

        return issueTokens(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        UserPasswordCredential credential = userPasswordCredentialRepository.findById(user.getId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    public TokenPairResponse refresh(String refreshToken) {
        return refreshTokenService.refresh(refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse issueTokens(User user) {
        TokenPairResponse tokenPair = refreshTokenService.issueForUser(user);
        return new AuthResponse(tokenPair.accessToken(), tokenPair.refreshToken(), UserDto.from(user));
    }

    private Role loadDefaultUserRole() {
        return roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new DefaultRoleNotConfiguredException(DEFAULT_ROLE_CODE));
    }
}
