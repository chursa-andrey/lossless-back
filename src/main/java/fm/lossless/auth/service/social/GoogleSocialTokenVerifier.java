package fm.lossless.auth.service.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import fm.lossless.auth.config.SocialAuthProperties;
import fm.lossless.auth.exception.InvalidSocialTokenException;
import fm.lossless.auth.exception.SocialAuthConfigurationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GoogleSocialTokenVerifier implements SocialTokenVerifier {

    private static final SocialProvider PROVIDER = SocialProvider.GOOGLE;

    private final RestClient restClient;
    private final SocialAuthProperties socialAuthProperties;

    public GoogleSocialTokenVerifier(RestClient.Builder restClientBuilder, SocialAuthProperties socialAuthProperties) {
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.socialAuthProperties = socialAuthProperties;
    }

    @Override
    public SocialProvider provider() {
        return PROVIDER;
    }

    @Override
    public SocialProviderProfile verify(String providerToken) {
        List<String> allowedClientIds = socialAuthProperties.getGoogle().getClientIds();
        if (allowedClientIds.isEmpty()) {
            throw new SocialAuthConfigurationException(PROVIDER);
        }

        GoogleTokenInfo tokenInfo;
        try {
            tokenInfo = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tokeninfo")
                            .queryParam("id_token", providerToken)
                            .build())
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (RestClientException ex) {
            throw new InvalidSocialTokenException();
        }

        if (tokenInfo == null
                || isBlank(tokenInfo.sub())
                || isBlank(tokenInfo.aud())
                || !allowedClientIds.contains(tokenInfo.aud())) {
            throw new InvalidSocialTokenException();
        }

        return new SocialProviderProfile(
                PROVIDER,
                tokenInfo.sub(),
                tokenInfo.email(),
                parseProviderBoolean(tokenInfo.emailVerified()),
                tokenInfo.name()
        );
    }

    private boolean parseProviderBoolean(Object source) {
        if (source instanceof Boolean value) {
            return value;
        }
        return source instanceof String value && Boolean.parseBoolean(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleTokenInfo(
            String sub,
            String aud,
            String email,
            @JsonProperty("email_verified") Object emailVerified,
            String name
    ) {
    }
}
