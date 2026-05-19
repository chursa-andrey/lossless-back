package fm.lossless.tracks.web.dto;

import fm.lossless.tracks.domain.Track;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record TrackFeedItemResponse(
        Long id,
        String title,
        String artistName,
        String albumTitle,
        String status,
        GenreResponse genre,
        TrackUserProfileResponse uploadedBy,
        String audioUrl,
        TrackAudioResponse audio,
        List<TrackPurchaseLinkResponse> purchaseLinks,
        Instant createdAt,
        Instant updatedAt
) {
    public static TrackFeedItemResponse from(Track track) {
        return new TrackFeedItemResponse(
                track.getId(),
                track.getTitle(),
                track.getArtistName(),
                track.getAlbumTitle(),
                track.getStatus().name(),
                GenreResponse.from(track.getGenre()),
                TrackUserProfileResponse.from(track.getCreatedBy()),
                "/api/v1/tracks/" + track.getId() + "/audio",
                TrackAudioResponse.from(track.getAudioFile()),
                track.getPurchaseLinks().stream()
                        .sorted(Comparator.comparingInt(link -> link.getPosition()))
                        .map(TrackPurchaseLinkResponse::from)
                        .toList(),
                track.getCreatedAt(),
                track.getUpdatedAt()
        );
    }
}
