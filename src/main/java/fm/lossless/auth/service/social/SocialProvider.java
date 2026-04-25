package fm.lossless.auth.service.social;

import fm.lossless.auth.exception.UnsupportedSocialProviderException;

import java.util.Arrays;
import java.util.Locale;

public enum SocialProvider {
    GOOGLE("google"),
    APPLE("apple"),
    FACEBOOK("facebook");

    private final String id;

    SocialProvider(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SocialProvider fromPathValue(String value) {
        if (value == null || value.isBlank()) {
            throw new UnsupportedSocialProviderException(value);
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(provider -> provider.id.equals(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new UnsupportedSocialProviderException(value));
    }
}
