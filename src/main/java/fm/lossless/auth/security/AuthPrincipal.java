package fm.lossless.auth.security;

import java.util.List;

public record AuthPrincipal(Long userId, List<String> roles) {
}
