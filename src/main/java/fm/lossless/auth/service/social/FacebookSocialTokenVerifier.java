package fm.lossless.auth.service.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import fm.lossless.auth.config.SocialAuthProperties;
import fm.lossless.auth.exception.InvalidSocialTokenException;
import fm.lossless.auth.exception.SocialAuthConfigurationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FacebookSocialTokenVerifier implements SocialTokenVerifier {

    private static final SocialProvider PROVIDER = SocialProvider.FACEBOOK;

    private final RestClient restClient;
    private final SocialAuthProperties socialAuthProperties;

    public FacebookSocialTokenVerifier(RestClient.Builder restClientBuilder, SocialAuthProperties socialAuthProperties) {
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com").build();
        this.socialAuthProperties = socialAuthProperties;
    }

    @Override
    public SocialProvider provider() {
        return PROVIDER;
    }

    @Override
    public SocialProviderProfile verify(String providerToken) {
        SocialAuthProperties.Facebook facebook = socialAuthProperties.getFacebook();
        if (!facebook.isConfigured()) {
            throw new SocialAuthConfigurationException(PROVIDER);
        }

        FacebookDebugResponse debugResponse;
        FacebookMeResponse meResponse;
        try {
            debugResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/debug_token")
                            .queryParam("input_token", providerToken)
                            .queryParam("access_token", facebook.appAccessToken())
                            .build())
                    .retrieve()
                    .body(FacebookDebugResponse.class);

            meResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/me")
                            .queryParam("fields", "id,name,email")
                            .queryParam("access_token", providerToken)
                            .build())
                    .retrieve()
                    .body(FacebookMeResponse.class);
        } catch (RestClientException ex) {
            throw new InvalidSocialTokenException();
        }

        FacebookDebugData debugData = debugResponse == null ? null : debugResponse.data();
        if (debugData == null
                || !debugData.valid()
                || isBlank(debugData.userId())
                || !facebook.getAppId().equals(debugData.appId())
                || meResponse == null
                || !debugData.userId().equals(meResponse.id())) {
            throw new InvalidSocialTokenException();
        }

        return new SocialProviderProfile(
                PROVIDER,
                debugData.userId(),
                meResponse.email(),
                !isBlank(meResponse.email()),
                meResponse.name()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record FacebookDebugResponse(FacebookDebugData data) {
    }

    private record FacebookDebugData(
            @JsonProperty("is_valid") boolean valid,
            @JsonProperty("app_id") String appId,
            @JsonProperty("user_id") String userId
    ) {
    }

    private record FacebookMeResponse(String id, String name, String email) {
    }
}
