package fm.lossless.tracks.exception;

import org.springframework.http.HttpStatus;

public class TrackUploadException extends TrackException {

    public TrackUploadException(HttpStatus status, TrackErrorCode code) {
        super(status, code);
    }

    public TrackUploadException(HttpStatus status, TrackErrorCode code, Throwable cause) {
        super(status, code, cause);
    }
}
