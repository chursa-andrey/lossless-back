package fm.lossless.auth.service.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import fm.lossless.auth.config.SocialAuthProperties;
import fm.lossless.auth.exception.InvalidSocialTokenException;
import fm.lossless.auth.exception.SocialAuthConfigurationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Component
public class AppleSocialTokenVerifier implements SocialTokenVerifier {

    private static final SocialProvider PROVIDER = SocialProvider.APPLE;
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String ALGORITHM = "RS256";
    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(6);

    private final RestClient restClient;
    private final SocialAuthProperties socialAuthProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private volatile AppleJwksCache jwksCache;

    public AppleSocialTokenVerifier(
            RestClient.Builder restClientBuilder,
            SocialAuthProperties socialAuthProperties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.restClient = restClientBuilder.baseUrl(ISSUER).build();
        this.socialAuthProperties = socialAuthProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public SocialProvider provider() {
        return PROVIDER;
    }

    @Override
    public SocialProviderProfile verify(String providerToken) {
        List<String> allowedClientIds = socialAuthProperties.getApple().getClientIds();
        if (allowedClientIds.isEmpty()) {
            throw new SocialAuthConfigurationException(PROVIDER);
        }

        String[] parts = providerToken.split("\\.");
        if (parts.length != 3) {
            throw new InvalidSocialTokenException();
        }

        try {
            AppleJwtHeader header = decodeJson(parts[0], AppleJwtHeader.class);
            if (!ALGORITHM.equals(header.alg()) || isBlank(header.kid())) {
                throw new InvalidSocialTokenException();
            }

            AppleJwk jwk = findJwk(header.kid());
            verifySignature(parts, jwk);

            AppleJwtClaims claims = decodeJson(parts[1], AppleJwtClaims.class);
            if (!ISSUER.equals(claims.iss())
                    || isBlank(claims.sub())
                    || !allowedClientIds.contains(claims.aud())
                    || claims.exp() <= Instant.now(clock).getEpochSecond()) {
                throw new InvalidSocialTokenException();
            }

            return new SocialProviderProfile(
                    PROVIDER,
                    claims.sub(),
                    claims.email(),
                    parseProviderBoolean(claims.emailVerified()),
                    null
            );
        } catch (InvalidSocialTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidSocialTokenException();
        }
    }

    private AppleJwk findJwk(String keyId) {
        return getJwks().keys().stream()
                .filter(key -> keyId.equals(key.kid()) && ALGORITHM.equals(key.alg()))
                .findFirst()
                .orElseThrow(InvalidSocialTokenException::new);
    }

    private AppleJwks getJwks() {
        Instant now = Instant.now(clock);
        AppleJwksCache currentCache = jwksCache;
        if (currentCache != null && now.isBefore(currentCache.expiresAt())) {
            return currentCache.jwks();
        }

        synchronized (this) {
            currentCache = jwksCache;
            if (currentCache != null && now.isBefore(currentCache.expiresAt())) {
                return currentCache.jwks();
            }

            try {
                AppleJwks jwks = restClient.get()
                        .uri("/auth/keys")
                        .retrieve()
                        .body(AppleJwks.class);
                if (jwks == null || jwks.keys() == null || jwks.keys().isEmpty()) {
                    throw new InvalidSocialTokenException();
                }
                jwksCache = new AppleJwksCache(jwks, now.plus(JWKS_CACHE_TTL));
                return jwks;
            } catch (RestClientException ex) {
                throw new InvalidSocialTokenException();
            }
        }
    }

    private void verifySignature(String[] tokenParts, AppleJwk jwk) throws GeneralSecurityException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(toPublicKey(jwk));
        signature.update((tokenParts[0] + "." + tokenParts[1]).getBytes(StandardCharsets.US_ASCII));
        if (!signature.verify(decodeBase64Url(tokenParts[2]))) {
            throw new InvalidSocialTokenException();
        }
    }

    private RSAPublicKey toPublicKey(AppleJwk jwk) throws GeneralSecurityException {
        BigInteger modulus = new BigInteger(1, decodeBase64Url(jwk.n()));
        BigInteger exponent = new BigInteger(1, decodeBase64Url(jwk.e()));
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private <T> T decodeJson(String value, Class<T> targetType) throws java.io.IOException {
        return objectMapper.readValue(decodeBase64Url(value), targetType);
    }

    private byte[] decodeBase64Url(String value) {
        return Base64.getUrlDecoder().decode(value);
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

    private record AppleJwtHeader(String kid, String alg) {
    }

    private record AppleJwtClaims(
            String iss,
            String aud,
            String sub,
            long exp,
            String email,
            @JsonProperty("email_verified") Object emailVerified
    ) {
    }

    private record AppleJwks(List<AppleJwk> keys) {
    }

    private record AppleJwk(String kid, String alg, String n, String e) {
    }

    private record AppleJwksCache(AppleJwks jwks, Instant expiresAt) {
    }
}
