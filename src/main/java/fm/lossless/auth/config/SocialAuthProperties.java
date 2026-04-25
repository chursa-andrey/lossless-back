package fm.lossless.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "auth.social")
public class SocialAuthProperties {

    private Provider google = new Provider();
    private Provider apple = new Provider();
    private Facebook facebook = new Facebook();

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

    public static class Provider {
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
}
