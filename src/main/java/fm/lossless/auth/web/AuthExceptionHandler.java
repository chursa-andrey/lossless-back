package fm.lossless.auth.web;

import fm.lossless.auth.exception.AuthFeatureNotImplementedException;
import fm.lossless.auth.exception.DefaultRoleNotConfiguredException;
import fm.lossless.auth.exception.EmailAlreadyInUseException;
import fm.lossless.auth.exception.InvalidCredentialsException;
import fm.lossless.auth.exception.InvalidRefreshTokenException;
import fm.lossless.auth.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, request.getRequestURI()));
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void handleEmailAlreadyInUse() {
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, request.getRequestURI()));
    }

    @ExceptionHandler(DefaultRoleNotConfiguredException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleDefaultRoleNotConfigured() {
    }

    @ExceptionHandler(AuthFeatureNotImplementedException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public void handleAuthFeatureNotImplemented() {
    }
}
