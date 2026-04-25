package fm.lossless.auth.service;

import fm.lossless.auth.domain.UserIdentity;
import fm.lossless.auth.exception.SocialEmailNotVerifiedException;
import fm.lossless.auth.exception.UnsupportedSocialProviderException;
import fm.lossless.auth.repo.UserIdentityRepository;
import fm.lossless.auth.service.social.SocialProvider;
import fm.lossless.auth.service.social.SocialProviderProfile;
import fm.lossless.auth.service.social.SocialTokenVerifier;
import fm.lossless.auth.web.dto.AuthResponse;
import fm.lossless.auth.web.dto.SocialAuthRequest;
import fm.lossless.auth.web.dto.TokenPairResponse;
import fm.lossless.users.domain.User;
import fm.lossless.users.repo.RoleRepository;
import fm.lossless.users.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialAuthServiceTest {

    private final UserIdentityRepository userIdentityRepository = mock(UserIdentityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final FakeSocialTokenVerifier tokenVerifier = new FakeSocialTokenVerifier();
    private final SocialAuthService socialAuthService = new SocialAuthService(
            userIdentityRepository,
            userRepository,
            roleRepository,
            refreshTokenService,
            List.of(tokenVerifier)
    );

    @Test
    void linksExistingUserByVerifiedEmailOnly() {
        User user = User.create("verified@example.com", "Verified User");
        tokenVerifier.profile = new SocialProviderProfile(
                SocialProvider.GOOGLE,
                "provider-subject",
                "Verified@Example.com",
                true,
                "Verified User"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("google", "provider-subject"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.issueForUser(user)).thenReturn(new TokenPairResponse("access", "refresh"));

        AuthResponse response = socialAuthService.loginWithProvider(
                "GOOGLE",
                new SocialAuthRequest("provider-token", null)
        );

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().email()).isEqualTo("verified@example.com");
        verify(userIdentityRepository).saveAndFlush(any(UserIdentity.class));
    }

    @Test
    void rejectsUnverifiedEmailBeforeLinkingByEmail() {
        tokenVerifier.profile = new SocialProviderProfile(
                SocialProvider.GOOGLE,
                "provider-subject",
                "unverified@example.com",
                false,
                "Unverified User"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("google", "provider-subject"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> socialAuthService.loginWithProvider(
                "google",
                new SocialAuthRequest("provider-token", null)
        )).isInstanceOf(SocialEmailNotVerifiedException.class);
    }

    @Test
    void resolvesConcurrentIdentityCreationByProviderSubject() {
        User user = User.create("race@example.com", "Race");
        UserIdentity identity = UserIdentity.create(user, "google", "provider-subject", "race@example.com");
        tokenVerifier.profile = new SocialProviderProfile(
                SocialProvider.GOOGLE,
                "provider-subject",
                "race@example.com",
                true,
                "Race"
        );

        when(userIdentityRepository.findByProviderAndProviderUserId("google", "provider-subject"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(identity));
        when(userRepository.findByEmail("race@example.com")).thenReturn(Optional.of(user));
        when(userIdentityRepository.saveAndFlush(any(UserIdentity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate identity"));
        when(refreshTokenService.issueForUser(user)).thenReturn(new TokenPairResponse("access", "refresh"));

        AuthResponse response = socialAuthService.loginWithProvider(
                "google",
                new SocialAuthRequest("provider-token", null)
        );

        assertThat(response.user().email()).isEqualTo("race@example.com");
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThatThrownBy(() -> socialAuthService.loginWithProvider(
                "unknown",
                new SocialAuthRequest("provider-token", null)
        )).isInstanceOf(UnsupportedSocialProviderException.class);
    }

    private static class FakeSocialTokenVerifier implements SocialTokenVerifier {
        private SocialProviderProfile profile;

        @Override
        public SocialProvider provider() {
            return SocialProvider.GOOGLE;
        }

        @Override
        public SocialProviderProfile verify(String providerToken) {
            return profile;
        }
    }
}
