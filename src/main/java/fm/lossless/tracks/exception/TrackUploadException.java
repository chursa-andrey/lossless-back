package fm.lossless.tracks.exception;

import org.springframework.http.HttpStatus;

public class TrackUploadException extends RuntimeException {

    private final HttpStatus status;
    private final TrackErrorCode code;

    public TrackUploadException(HttpStatus status, TrackErrorCode code) {
        super(code.name());
        this.status = status;
        this.code = code;
    }

    public TrackUploadException(HttpStatus status, TrackErrorCode code, Throwable cause) {
        super(code.name(), cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public TrackErrorCode getCode() {
        return code;
    }
}
