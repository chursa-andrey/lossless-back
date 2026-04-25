package fm.lossless.auth.exception;

import fm.lossless.auth.service.social.SocialProvider;

public class SocialAuthConfigurationException extends RuntimeException {

    public SocialAuthConfigurationException(String provider) {
        super("Social auth provider is not configured: " + provider);
    }

    public SocialAuthConfigurationException(SocialProvider provider) {
        super("Social auth provider is not configured: " + provider.id());
    }
}
