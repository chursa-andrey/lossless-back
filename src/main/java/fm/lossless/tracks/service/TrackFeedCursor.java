package fm.lossless.tracks.service;

import fm.lossless.tracks.exception.TrackErrorCode;
import fm.lossless.tracks.exception.TrackException;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record TrackFeedCursor(
        Instant createdAt,
        Long id
) {
    public static TrackFeedCursor from(Instant createdAt, Long id) {
        if (createdAt == null && id == null) {
            return null;
        }
        if (createdAt != null && id != null) {
            return new TrackFeedCursor(createdAt, id);
        }
        throw new TrackException(HttpStatus.BAD_REQUEST, TrackErrorCode.TRACK_FEED_CURSOR_INVALID);
    }
}
