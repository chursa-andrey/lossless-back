package fm.lossless.tracks.storage;

import java.nio.file.Path;

public record StoredTrackFile(
        String storageProvider,
        String storageKey,
        long sizeBytes,
        String checksumSha256,
        Path metadataSourcePath
) {
}
