package fm.lossless.auth.service;

import fm.lossless.auth.exception.DefaultRoleNotConfiguredException;
import fm.lossless.auth.exception.EmailAlreadyInUseException;
import fm.lossless.auth.exception.InvalidCredentialsException;
import fm.lossless.auth.exception.PasswordLoginNotAvailableException;
import fm.lossless.auth.domain.UserPasswordCredential;
import fm.lossless.auth.repo.UserPasswordCredentialRepository;
import fm.lossless.auth.web.dto.AuthResponse;
import fm.lossless.auth.web.dto.ContinueAuthRequest;
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

        return createUserWithPassword(normalizedEmail, request.password(), request.displayName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        return authenticateExistingUser(user, request.password());
    }

    @Transactional
    public AuthResponse continueWithEmail(ContinueAuthRequest request) {
        String normalizedEmail = User.normalizeEmail(request.email());
        return userRepository.findByEmail(normalizedEmail)
                .map(user -> authenticateExistingUser(user, request.password()))
                .orElseGet(() -> createOrResolveRacingUser(normalizedEmail, request.password(), request.displayName()));
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

    private AuthResponse createOrResolveRacingUser(String normalizedEmail, String password, String displayName) {
        try {
            return createUserWithPassword(normalizedEmail, password, displayName);
        } catch (EmailAlreadyInUseException ex) {
            User existingUser = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(InvalidCredentialsException::new);
            return authenticateExistingUser(existingUser, password);
        }
    }

    private AuthResponse createUserWithPassword(String normalizedEmail, String rawPassword, String displayName) {
        User user = User.create(normalizedEmail, displayName);
        user.addRole(loadDefaultUserRole());
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Handles race condition when two requests register the same email concurrently.
            throw new EmailAlreadyInUseException();
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        UserPasswordCredential credential = UserPasswordCredential.create(savedUser, passwordHash);
        userPasswordCredentialRepository.save(credential);

        return issueTokens(savedUser);
    }

    private AuthResponse authenticateExistingUser(User user, String rawPassword) {
        UserPasswordCredential credential = userPasswordCredentialRepository.findById(user.getId())
                .orElseThrow(PasswordLoginNotAvailableException::new);

        if (!passwordEncoder.matches(rawPassword, credential.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    private Role loadDefaultUserRole() {
        return roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new DefaultRoleNotConfiguredException(DEFAULT_ROLE_CODE));
    }
}
