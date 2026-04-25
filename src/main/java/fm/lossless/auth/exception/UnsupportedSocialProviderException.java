package fm.lossless.auth.exception;

import fm.lossless.auth.service.social.SocialProvider;

public class UnsupportedSocialProviderException extends RuntimeException {

    public UnsupportedSocialProviderException(String provider) {
        super("Unsupported social auth provider: " + provider);
    }

    public UnsupportedSocialProviderException(SocialProvider provider) {
        super("Unsupported social auth provider: " + provider.id());
    }
}
