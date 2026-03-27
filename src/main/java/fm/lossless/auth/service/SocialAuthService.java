package fm.lossless.auth.service;

import fm.lossless.auth.exception.AuthFeatureNotImplementedException;
import fm.lossless.auth.web.dto.AuthResponse;
import org.springframework.stereotype.Service;

@Service
public class SocialAuthService {

    public AuthResponse loginWithProvider(String provider, String providerToken) {
        throw new AuthFeatureNotImplementedException(
                "Social auth provider '%s' is not implemented yet".formatted(provider)
        );
    }
}
