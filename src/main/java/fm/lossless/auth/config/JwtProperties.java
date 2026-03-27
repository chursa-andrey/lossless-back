package fm.lossless.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String issuer;
    private String secret;
    private long accessTtlMinutes;
    private long refreshTtlDays;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(long accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(long refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }
}
