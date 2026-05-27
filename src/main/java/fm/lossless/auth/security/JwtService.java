package fm.lossless.auth.security;

import fm.lossless.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class JwtService {

    private final JwtProperties props;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;

        String secret = props.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be set.");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret is too short for HS256. Use at least 256-bit (32 bytes) key.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);

        if (props.getAccessTtlMinutes() <= 0) {
            throw new IllegalArgumentException("Access token TTL must be positive.");
        }
        if (props.getIssuer() == null || props.getIssuer().isBlank()) {
            throw new IllegalArgumentException("JWT issuer must be set.");
        }
    }

    public String createAccessToken(Long userId, List<String> roles) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(props.getAccessTtlMinutes() * 60);

        List<String> safeRoles = normalizeRoles(roles);

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("roles", safeRoles)
                .signWith(key)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        Set<String> out = new LinkedHashSet<>();
        for (String r : roles) {
            if (r == null) {
                continue;
            }
            String s = r.trim();
            if (s.isEmpty()) {
                continue;
            }
            out.add(s.toUpperCase(Locale.ROOT));
        }
        return List.copyOf(out);
    }
}
