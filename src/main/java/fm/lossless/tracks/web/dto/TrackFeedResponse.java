package fm.lossless.tracks.web.dto;

import java.util.List;

public record TrackFeedResponse(
        List<TrackFeedItemResponse> items,
        TrackFeedCursorResponse nextCursor,
        boolean hasMore
) {
}
