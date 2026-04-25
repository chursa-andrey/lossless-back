package fm.lossless.auth.service;

import fm.lossless.auth.domain.UserIdentity;
import fm.lossless.auth.exception.DefaultRoleNotConfiguredException;
import fm.lossless.auth.exception.InvalidSocialTokenException;
import fm.lossless.auth.exception.SocialEmailNotVerifiedException;
import fm.lossless.auth.exception.SocialEmailRequiredException;
import fm.lossless.auth.exception.UnsupportedSocialProviderException;
import fm.lossless.auth.repo.UserIdentityRepository;
import fm.lossless.auth.service.social.SocialProvider;
import fm.lossless.auth.service.social.SocialProviderProfile;
import fm.lossless.auth.service.social.SocialTokenVerifier;
import fm.lossless.auth.web.dto.AuthResponse;
import fm.lossless.auth.web.dto.SocialAuthRequest;
import fm.lossless.auth.web.dto.TokenPairResponse;
import fm.lossless.users.domain.Role;
import fm.lossless.users.domain.User;
import fm.lossless.users.repo.RoleRepository;
import fm.lossless.users.repo.UserRepository;
import fm.lossless.users.web.dto.UserDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SocialAuthService {

    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserIdentityRepository userIdentityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final Map<SocialProvider, SocialTokenVerifier> tokenVerifiers;

    public SocialAuthService(
            UserIdentityRepository userIdentityRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenService refreshTokenService,
            List<SocialTokenVerifier> tokenVerifiers
    ) {
        this.userIdentityRepository = userIdentityRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenService = refreshTokenService;
        this.tokenVerifiers = tokenVerifiers.stream()
                .collect(Collectors.toUnmodifiableMap(SocialTokenVerifier::provider, Function.identity()));
    }

    @Transactional
    public AuthResponse loginWithProvider(String provider, SocialAuthRequest request) {
        SocialProvider socialProvider = SocialProvider.fromPathValue(provider);
        SocialTokenVerifier tokenVerifier = tokenVerifiers.get(socialProvider);
        if (tokenVerifier == null) {
            throw new UnsupportedSocialProviderException(socialProvider);
        }

        SocialProviderProfile profile = tokenVerifier.verify(request.providerToken());
        if (profile.provider() != socialProvider || isBlank(profile.providerUserId())) {
            throw new InvalidSocialTokenException();
        }

        return userIdentityRepository
                .findByProviderAndProviderUserId(profile.provider().id(), profile.providerUserId())
                .map(identity -> issueTokens(identity.getUser()))
                .orElseGet(() -> createOrLinkIdentity(profile, request.displayName()));
    }

    private AuthResponse createOrLinkIdentity(SocialProviderProfile profile, String fallbackDisplayName) {
        String normalizedEmail = User.normalizeEmail(profile.email());
        if (isBlank(normalizedEmail)) {
            throw new SocialEmailRequiredException();
        }
        if (!profile.emailVerified()) {
            throw new SocialEmailNotVerifiedException();
        }

        User user = findOrCreateUser(normalizedEmail, resolveDisplayName(profile, fallbackDisplayName));
        try {
            UserIdentity identity = UserIdentity.create(
                    user,
                    profile.provider().id(),
                    profile.providerUserId(),
                    normalizedEmail
            );
            userIdentityRepository.saveAndFlush(identity);
            return issueTokens(user);
        } catch (DataIntegrityViolationException ex) {
            return userIdentityRepository
                    .findByProviderAndProviderUserId(profile.provider().id(), profile.providerUserId())
                    .map(identity -> issueTokens(identity.getUser()))
                    .orElseThrow(() -> ex);
        }
    }

    private User findOrCreateUser(String normalizedEmail, String displayName) {
        return userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createSocialUser(normalizedEmail, displayName));
    }

    private User createSocialUser(String normalizedEmail, String displayName) {
        User user = User.create(normalizedEmail, displayName);
        user.addRole(loadDefaultUserRole());
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            return userRepository.findByEmail(normalizedEmail).orElseThrow(() -> ex);
        }
    }

    private AuthResponse issueTokens(User user) {
        TokenPairResponse tokenPair = refreshTokenService.issueForUser(user);
        return new AuthResponse(tokenPair.accessToken(), tokenPair.refreshToken(), UserDto.from(user));
    }

    private Role loadDefaultUserRole() {
        return roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new DefaultRoleNotConfiguredException(DEFAULT_ROLE_CODE));
    }

    private String resolveDisplayName(SocialProviderProfile profile, String fallbackDisplayName) {
        if (!isBlank(profile.displayName())) {
            return profile.displayName();
        }
        return fallbackDisplayName;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
