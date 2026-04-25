package fm.lossless.auth.web;

import fm.lossless.auth.exception.AuthErrorCode;
import fm.lossless.auth.exception.AuthFeatureNotImplementedException;
import fm.lossless.auth.exception.DefaultRoleNotConfiguredException;
import fm.lossless.auth.exception.EmailAlreadyInUseException;
import fm.lossless.auth.exception.InvalidCredentialsException;
import fm.lossless.auth.exception.InvalidRefreshTokenException;
import fm.lossless.auth.exception.InvalidSocialTokenException;
import fm.lossless.auth.exception.PasswordLoginNotAvailableException;
import fm.lossless.auth.exception.SocialAuthConfigurationException;
import fm.lossless.auth.exception.SocialEmailNotVerifiedException;
import fm.lossless.auth.exception.SocialEmailRequiredException;
import fm.lossless.auth.exception.UnsupportedSocialProviderException;
import fm.lossless.auth.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, AuthErrorCode.VALIDATION_FAILED, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_CREDENTIALS, request);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyInUse(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, AuthErrorCode.EMAIL_ALREADY_IN_USE, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_REFRESH_TOKEN, request);
    }

    @ExceptionHandler(PasswordLoginNotAvailableException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordLoginNotAvailable(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, AuthErrorCode.PASSWORD_LOGIN_NOT_AVAILABLE, request);
    }

    @ExceptionHandler(UnsupportedSocialProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedSocialProvider(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, AuthErrorCode.UNSUPPORTED_SOCIAL_PROVIDER, request);
    }

    @ExceptionHandler(InvalidSocialTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSocialToken(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, AuthErrorCode.INVALID_SOCIAL_TOKEN, request);
    }

    @ExceptionHandler(SocialEmailRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleSocialEmailRequired(HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, AuthErrorCode.SOCIAL_EMAIL_REQUIRED, request);
    }

    @ExceptionHandler(SocialEmailNotVerifiedException.class)
    public ResponseEntity<ApiErrorResponse> handleSocialEmailNotVerified(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, AuthErrorCode.SOCIAL_EMAIL_NOT_VERIFIED, request);
    }

    @ExceptionHandler(DefaultRoleNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> handleDefaultRoleNotConfigured(HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, AuthErrorCode.DEFAULT_ROLE_NOT_CONFIGURED, request);
    }

    @ExceptionHandler(SocialAuthConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> handleSocialAuthConfiguration(HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, AuthErrorCode.SOCIAL_AUTH_MISCONFIGURED, request);
    }

    @ExceptionHandler(AuthFeatureNotImplementedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthFeatureNotImplemented(HttpServletRequest request) {
        return error(HttpStatus.NOT_IMPLEMENTED, AuthErrorCode.AUTH_FEATURE_NOT_IMPLEMENTED, request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, AuthErrorCode code, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status, code, request.getRequestURI()));
    }
}
