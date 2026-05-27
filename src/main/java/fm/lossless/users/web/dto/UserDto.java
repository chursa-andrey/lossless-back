package fm.lossless.users.web.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import fm.lossless.users.domain.User;

public record UserDto(Long id, String email, String displayName, List<String> roles, Instant createdAt) {
    public static UserDto from(User u) {
        List<String> roles = u.getRoles().stream()
                .map(role -> role.getCode())
                .sorted(Comparator.naturalOrder())
                .toList();
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), roles, u.getCreatedAt());
    }
}
