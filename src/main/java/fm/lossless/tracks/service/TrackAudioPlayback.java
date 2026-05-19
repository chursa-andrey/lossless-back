package fm.lossless.tracks.service;

import org.springframework.core.io.Resource;

public record TrackAudioPlayback(
        Resource resource,
        long sizeBytes,
        String originalFilename,
        String extension
) {
}
