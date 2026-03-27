package fm.lossless.auth.web.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String path
) {
    public static ApiErrorResponse of(HttpStatus status, String path) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), path);
    }
}
