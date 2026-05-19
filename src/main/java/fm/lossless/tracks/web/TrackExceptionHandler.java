package fm.lossless.tracks.web;

import fm.lossless.auth.web.dto.ApiErrorResponse;
import fm.lossless.tracks.exception.TrackErrorCode;
import fm.lossless.tracks.exception.TrackException;
import fm.lossless.tracks.exception.TrackUploadException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class TrackExceptionHandler {

    @ExceptionHandler(TrackException.class)
    public ResponseEntity<ApiErrorResponse> handleTrack(TrackException ex, HttpServletRequest request) {
        return error(ex.getStatus(), ex.getCode(), request);
    }

    @ExceptionHandler(TrackUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleTrackUpload(TrackUploadException ex, HttpServletRequest request) {
        return error(ex.getStatus(), ex.getCode(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        TrackErrorCode code = "file".equals(ex.getRequestPartName())
                ? TrackErrorCode.TRACK_FILE_REQUIRED
                : TrackErrorCode.TRACK_UPLOAD_FAILED;
        return error(HttpStatus.UNPROCESSABLE_ENTITY, code, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        TrackErrorCode code = "genre".equals(ex.getParameterName())
                ? TrackErrorCode.TRACK_GENRE_REQUIRED
                : TrackErrorCode.TRACK_UPLOAD_FAILED;
        return error(HttpStatus.UNPROCESSABLE_ENTITY, code, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, TrackErrorCode.TRACK_FILE_TOO_LARGE, request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, TrackErrorCode.TRACK_UPLOAD_FAILED, request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            TrackErrorCode code,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status, code.name(), request.getRequestURI()));
    }
}
