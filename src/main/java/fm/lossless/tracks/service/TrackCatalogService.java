package fm.lossless.tracks.service;

import fm.lossless.tracks.config.TrackFeedProperties;
import fm.lossless.tracks.domain.Track;
import fm.lossless.tracks.domain.TrackAudioFile;
import fm.lossless.tracks.domain.TrackStatus;
import fm.lossless.tracks.exception.TrackErrorCode;
import fm.lossless.tracks.exception.TrackException;
import fm.lossless.tracks.repo.TrackRepository;
import fm.lossless.tracks.storage.StoredTrackResource;
import fm.lossless.tracks.storage.TrackStorageService;
import fm.lossless.tracks.web.dto.TrackFeedCursorResponse;
import fm.lossless.tracks.web.dto.TrackFeedItemResponse;
import fm.lossless.tracks.web.dto.TrackFeedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrackCatalogService {

    private final TrackFeedProperties feedProperties;
    private final TrackRepository trackRepository;
    private final TrackStorageService storageService;

    public TrackCatalogService(
            TrackFeedProperties feedProperties,
            TrackRepository trackRepository,
            TrackStorageService storageService
    ) {
        this.feedProperties = feedProperties;
        this.trackRepository = trackRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public TrackFeedResponse getFeed(Integer limit, Instant cursorCreatedAt, Long cursorId) {
        TrackFeedCursor cursor = TrackFeedCursor.from(cursorCreatedAt, cursorId);
        int pageSize = normalizeLimit(limit);
        List<Long> ids = findFeedIds(cursor, PageRequest.of(0, pageSize + 1));
        boolean hasMore = ids.size() > pageSize;
        List<Long> pageIds = hasMore ? ids.subList(0, pageSize) : ids;
        if (pageIds.isEmpty()) {
            return new TrackFeedResponse(List.of(), null, false);
        }

        List<TrackFeedItemResponse> items = orderByFeedIds(trackRepository.findFeedItemsByIds(pageIds), pageIds)
                .stream()
                .map(TrackFeedItemResponse::from)
                .toList();

        TrackFeedCursorResponse nextCursor = hasMore
                ? new TrackFeedCursorResponse(items.get(items.size() - 1).createdAt(), items.get(items.size() - 1).id())
                : null;

        return new TrackFeedResponse(items, nextCursor, hasMore);
    }

    private List<Long> findFeedIds(TrackFeedCursor cursor, PageRequest pageRequest) {
        if (cursor == null) {
            return trackRepository.findInitialFeedIds(TrackStatus.UPLOADED, pageRequest);
        }

        return trackRepository.findFeedIdsAfterCursor(
                TrackStatus.UPLOADED,
                cursor.createdAt(),
                cursor.id(),
                pageRequest
        );
    }

    @Transactional(readOnly = true)
    public TrackAudioPlayback getAudio(Long trackId) {
        Track track = trackRepository.findWithAudioFileById(trackId)
                .orElseThrow(() -> new TrackException(HttpStatus.NOT_FOUND, TrackErrorCode.TRACK_NOT_FOUND));
        TrackAudioFile audioFile = track.getAudioFile();
        if (audioFile == null) {
            throw new TrackException(HttpStatus.NOT_FOUND, TrackErrorCode.TRACK_AUDIO_NOT_FOUND);
        }

        StoredTrackResource storedResource = storageService.load(audioFile.getStorageKey());
        return new TrackAudioPlayback(
                storedResource.resource(),
                storedResource.sizeBytes(),
                audioFile.getOriginalFilename(),
                audioFile.getExtension()
        );
    }

    private List<Track> orderByFeedIds(List<Track> tracks, List<Long> ids) {
        Map<Long, Integer> orderById = new HashMap<>();
        for (int index = 0; index < ids.size(); index++) {
            orderById.put(ids.get(index), index);
        }
        return tracks.stream()
                .sorted(Comparator.comparingInt(track -> orderById.getOrDefault(track.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        int defaultPageSize = Math.max(1, feedProperties.getDefaultPageSize());
        int maxPageSize = Math.max(defaultPageSize, feedProperties.getMaxPageSize());
        if (limit == null) {
            return defaultPageSize;
        }
        if (limit <= 0) {
            throw new TrackException(HttpStatus.BAD_REQUEST, TrackErrorCode.TRACK_FEED_LIMIT_INVALID);
        }
        return Math.min(limit, maxPageSize);
    }
}
