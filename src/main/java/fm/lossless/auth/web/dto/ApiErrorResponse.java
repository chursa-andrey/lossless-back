package fm.lossless.auth.web.dto;

import fm.lossless.auth.exception.AuthErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String path
) {
    public static ApiErrorResponse of(HttpStatus status, AuthErrorCode code, String path) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code.name(), path);
    }
}
