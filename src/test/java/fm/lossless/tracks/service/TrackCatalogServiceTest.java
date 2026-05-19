package fm.lossless.tracks.service;

import fm.lossless.tracks.config.TrackFeedProperties;
import fm.lossless.tracks.domain.Genre;
import fm.lossless.tracks.domain.Track;
import fm.lossless.tracks.domain.TrackStatus;
import fm.lossless.tracks.exception.TrackException;
import fm.lossless.tracks.repo.TrackRepository;
import fm.lossless.tracks.storage.TrackStorageService;
import fm.lossless.tracks.web.dto.TrackFeedResponse;
import fm.lossless.users.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrackCatalogServiceTest {

    private final TrackRepository trackRepository = mock(TrackRepository.class);
    private final TrackStorageService storageService = mock(TrackStorageService.class);
    private final TrackFeedProperties feedProperties = new TrackFeedProperties();
    private final TrackCatalogService service = new TrackCatalogService(
            feedProperties,
            trackRepository,
            storageService
    );

    @Test
    void getFeedRestoresOrderFromFeedIds() {
        Track third = track(3L);
        Track second = track(2L);
        Track first = track(1L);

        when(trackRepository.findInitialFeedIds(eq(TrackStatus.UPLOADED), any(Pageable.class)))
                .thenReturn(List.of(3L, 2L, 1L));
        when(trackRepository.findFeedItemsByIds(List.of(3L, 2L, 1L)))
                .thenReturn(List.of(first, third, second));

        TrackFeedResponse response = service.getFeed(3, null, null);

        assertThat(response.items())
                .extracting(item -> item.id())
                .containsExactly(3L, 2L, 1L);
    }

    @Test
    void getFeedUsesCursorQueryOnlyWhenCursorIsPresent() {
        Instant cursorCreatedAt = Instant.parse("2026-05-17T00:00:00Z");

        when(trackRepository.findFeedIdsAfterCursor(
                eq(TrackStatus.UPLOADED),
                eq(cursorCreatedAt),
                eq(10L),
                any(Pageable.class)
        )).thenReturn(List.of());

        TrackFeedResponse response = service.getFeed(3, cursorCreatedAt, 10L);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void getFeedRejectsPartialCursor() {
        assertThatThrownBy(() -> service.getFeed(null, Instant.parse("2026-05-17T00:00:00Z"), null))
                .isInstanceOf(TrackException.class)
                .hasMessage("TRACK_FEED_CURSOR_INVALID");
    }

    private Track track(Long id) {
        Genre genre = mock(Genre.class);
        when(genre.getSlug()).thenReturn("rock");
        when(genre.getName()).thenReturn("Rock");

        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getDisplayName()).thenReturn("Uploader");

        Track track = mock(Track.class);
        when(track.getId()).thenReturn(id);
        when(track.getTitle()).thenReturn("Track " + id);
        when(track.getStatus()).thenReturn(TrackStatus.UPLOADED);
        when(track.getGenre()).thenReturn(genre);
        when(track.getCreatedBy()).thenReturn(user);
        when(track.getPurchaseLinks()).thenReturn(List.of());
        when(track.getCreatedAt()).thenReturn(Instant.parse("2026-05-17T00:00:00Z"));
        when(track.getUpdatedAt()).thenReturn(Instant.parse("2026-05-17T00:00:00Z"));
        return track;
    }
}
