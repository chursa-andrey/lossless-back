package fm.lossless.tracks.web.dto;

import java.time.Instant;

public record TrackFeedCursorResponse(
        Instant createdAt,
        Long id
) {
}
