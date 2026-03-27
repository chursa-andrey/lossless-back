package fm.lossless.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank @Size(min = 32, max = 512) String refreshToken
) {
}
