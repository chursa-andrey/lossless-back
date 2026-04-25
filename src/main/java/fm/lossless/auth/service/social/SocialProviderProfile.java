package fm.lossless.auth.service.social;

public record SocialProviderProfile(
        SocialProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName
) {
}
