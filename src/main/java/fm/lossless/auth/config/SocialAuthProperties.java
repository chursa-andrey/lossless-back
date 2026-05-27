package fm.lossless.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "auth.social")
@Validated
public class SocialAuthProperties {

    @Valid
    @NotNull
    private Provider google = new Provider();

    @Valid
    @NotNull
    private Provider apple = new Provider();

    @Valid
    @NotNull
    private Facebook facebook = new Facebook();

    @Valid
    @NotNull
    private Http http = new Http();

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider getApple() {
        return apple;
    }

    public void setApple(Provider apple) {
        this.apple = apple;
    }

    public Facebook getFacebook() {
        return facebook;
    }

    public void setFacebook(Facebook facebook) {
        this.facebook = facebook;
    }

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public static class Provider {
        @NotNull
        private List<String> clientIds = new ArrayList<>();

        public List<String> getClientIds() {
            return clientIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .toList();
        }

        public void setClientIds(List<String> clientIds) {
            this.clientIds = clientIds == null ? new ArrayList<>() : clientIds;
        }
    }

    public static class Facebook {
        private String appId;
        private String appSecret;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public boolean isConfigured() {
            return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
        }

        public String appAccessToken() {
            return appId + "|" + appSecret;
        }
    }

    public static class Http {
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(3);

        @NotNull
        private Duration readTimeout = Duration.ofSeconds(5);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        @AssertTrue(message = "connectTimeout must be positive")
        public boolean isConnectTimeoutPositive() {
            return connectTimeout != null && !connectTimeout.isZero() && !connectTimeout.isNegative();
        }

        @AssertTrue(message = "readTimeout must be positive")
        public boolean isReadTimeoutPositive() {
            return readTimeout != null && !readTimeout.isZero() && !readTimeout.isNegative();
        }
    }
}
