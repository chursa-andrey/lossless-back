package fm.lossless.tracks.web;

import fm.lossless.auth.security.AuthPrincipal;
import fm.lossless.tracks.service.GenreService;
import fm.lossless.tracks.service.TrackAudioPlayback;
import fm.lossless.tracks.service.TrackCatalogService;
import fm.lossless.tracks.service.TrackUploadService;
import fm.lossless.tracks.web.dto.GenreResponse;
import fm.lossless.tracks.web.dto.TrackFeedResponse;
import fm.lossless.tracks.web.dto.UploadTrackResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<Resource> getTrackAudio(@PathVariable Long trackId) {
        TrackAudioPlayback playback = trackCatalogService.getAudio(trackId);
        String filename = resolvePlaybackFilename(trackId, playback);

        return ResponseEntity.ok()
                .contentType(resolveMediaType(playback.extension()))
                .contentLength(playback.sizeBytes())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(playback.resource());
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

    private String resolvePlaybackFilename(Long trackId, TrackAudioPlayback playback) {
        if (playback.originalFilename() != null && !playback.originalFilename().isBlank()) {
            return playback.originalFilename();
        }
        return "track-" + trackId + "." + playback.extension();
    }
}
