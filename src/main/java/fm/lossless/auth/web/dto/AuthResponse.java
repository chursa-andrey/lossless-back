package fm.lossless.auth.web.dto;

import fm.lossless.users.web.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {
}
