package fm.lossless.tracks.storage;

import org.springframework.core.io.Resource;

public record StoredTrackResource(
        Resource resource,
        long sizeBytes
) {
}
