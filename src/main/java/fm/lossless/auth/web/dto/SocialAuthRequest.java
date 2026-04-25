package fm.lossless.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SocialAuthRequest(
        @NotBlank @Size(max = 4096) String providerToken,
        @Size(max = 100) String displayName
) {
}
