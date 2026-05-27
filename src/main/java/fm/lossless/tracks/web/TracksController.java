package fm.lossless.tracks.web;

import fm.lossless.auth.security.AuthPrincipal;
import fm.lossless.tracks.service.GenreService;
import fm.lossless.tracks.service.TrackAudioPlayback;
import fm.lossless.tracks.service.TrackCatalogService;
import fm.lossless.tracks.service.TrackUploadService;
import fm.lossless.tracks.exception.TrackErrorCode;
import fm.lossless.tracks.exception.TrackException;
import fm.lossless.tracks.web.dto.GenreResponse;
import fm.lossless.tracks.web.dto.TrackFeedResponse;
import fm.lossless.tracks.web.dto.UploadTrackResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tracks")
public class TracksController {

    private final TrackUploadService trackUploadService;
    private final TrackCatalogService trackCatalogService;
    private final GenreService genreService;

    public TracksController(
            TrackUploadService trackUploadService,
            TrackCatalogService trackCatalogService,
            GenreService genreService
    ) {
        this.trackUploadService = trackUploadService;
        this.trackCatalogService = trackCatalogService;
        this.genreService = genreService;
    }

    @GetMapping
    public ResponseEntity<TrackFeedResponse> getTracks(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "cursorCreatedAt", required = false) Instant cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) Long cursorId
    ) {
        return ResponseEntity.ok(trackCatalogService.getFeed(limit, cursorCreatedAt, cursorId));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreResponse>> getGenres() {
        return ResponseEntity.ok(genreService.getActiveGenres().stream()
                .map(GenreResponse::from)
                .toList());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadTrackResponse> uploadTrack(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("genre") String genre,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "artistName", required = false) String artistName,
            @RequestParam(name = "albumTitle", required = false) String albumTitle,
            @RequestParam(name = "purchaseLinks[]", required = false) List<String> purchaseLinks,
            @RequestParam(name = "purchaseLinks", required = false) List<String> purchaseLinksWithoutBrackets
    ) {
        Long trackId = trackUploadService.upload(
                principal,
                file,
                genre,
                title,
                artistName,
                albumTitle,
                mergePurchaseLinks(purchaseLinks, purchaseLinksWithoutBrackets)
        );
        return ResponseEntity.created(URI.create("/api/v1/tracks/" + trackId))
                .body(new UploadTrackResponse(trackId));
    }

    @GetMapping("/{trackId}/audio")
    public ResponseEntity<?> getTrackAudio(
            @PathVariable Long trackId,
            @RequestHeader HttpHeaders headers
    ) {
        TrackAudioPlayback playback = trackCatalogService.getAudio(trackId);
        String filename = resolvePlaybackFilename(trackId, playback);
        HttpHeaders responseHeaders = audioHeaders(playback, filename);

        if (headers.getRange().isEmpty()) {
            responseHeaders.setContentLength(playback.sizeBytes());
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(playback.resource());
        }

        ResolvedAudioRange range = resolveSingleRange(headers.getRange(), playback.sizeBytes());
        responseHeaders.setContentLength(range.length());
        responseHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes %d-%d/%d".formatted(
                range.start(),
                range.end(),
                playback.sizeBytes()
        ));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(responseHeaders)
                .body(rangeResource(playback.resource(), range));
    }

    private List<String> mergePurchaseLinks(List<String> primary, List<String> secondary) {
        List<String> result = new ArrayList<>();
        if (primary != null) {
            result.addAll(primary);
        }
        if (secondary != null) {
            result.addAll(secondary);
        }
        return result;
    }

    private MediaType resolveMediaType(String extension) {
        if ("wav".equalsIgnoreCase(extension)) {
            return MediaType.parseMediaType("audio/wav");
        }
        if ("flac".equalsIgnoreCase(extension)) {
            return MediaType.parseMediaType("audio/flac");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private HttpHeaders audioHeaders(TrackAudioPlayback playback, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(playback.extension()));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return headers;
    }

    private ResolvedAudioRange resolveSingleRange(List<HttpRange> ranges, long sizeBytes) {
        if (ranges.size() != 1 || sizeBytes <= 0) {
            throw new TrackException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, TrackErrorCode.TRACK_AUDIO_RANGE_INVALID);
        }

        try {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(sizeBytes);
            long end = range.getRangeEnd(sizeBytes);
            long rangeLength = Math.min(end - start + 1, sizeBytes - start);

            if (start < 0 || end < start || rangeLength <= 0) {
                throw new TrackException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                        TrackErrorCode.TRACK_AUDIO_RANGE_INVALID);
            }

            return new ResolvedAudioRange(start, start + rangeLength - 1, rangeLength);
        } catch (IllegalArgumentException ex) {
            throw new TrackException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                    TrackErrorCode.TRACK_AUDIO_RANGE_INVALID, ex);
        }
    }

    private InputStreamResource rangeResource(Resource resource, ResolvedAudioRange range) {
        try {
            InputStream input = resource.getInputStream();
            input.skipNBytes(range.start());
            return new InputStreamResource(new BoundedInputStream(input, range.length()));
        } catch (IOException ex) {
            throw new TrackException(HttpStatus.INTERNAL_SERVER_ERROR, TrackErrorCode.TRACK_STORAGE_FAILED, ex);
        }
    }

    private String resolvePlaybackFilename(Long trackId, TrackAudioPlayback playback) {
        if (playback.originalFilename() != null && !playback.originalFilename().isBlank()) {
            return playback.originalFilename();
        }
        return "track-" + trackId + "." + playback.extension();
    }

    private record ResolvedAudioRange(long start, long end, long length) {
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private long remaining;

        private BoundedInputStream(InputStream input, long length) {
            super(input);
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (read != -1) {
                remaining -= read;
            }
            return read;
        }
    }
}
