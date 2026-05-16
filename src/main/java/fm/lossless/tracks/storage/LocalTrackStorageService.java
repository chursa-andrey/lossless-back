package fm.lossless.tracks.storage;

import fm.lossless.tracks.config.StorageProperties;
import fm.lossless.tracks.exception.TrackErrorCode;
import fm.lossless.tracks.exception.TrackUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class LocalTrackStorageService implements TrackStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalTrackStorageService.class);
    private static final String STORAGE_PROVIDER = "LOCAL";

    private final StorageProperties storageProperties;
    private final Clock clock;

    public LocalTrackStorageService(StorageProperties storageProperties, Clock clock) {
        this.storageProperties = storageProperties;
        this.clock = clock;
    }

    @Override
    public StoredTrackFile store(MultipartFile file, String extension) {
        String storageKey = buildStorageKey(extension);
        Path rootPath = storageProperties.getRootPath().toAbsolutePath().normalize();
        Path target = rootPath.resolve(storageKey).normalize();

        if (!target.startsWith(rootPath)) {
            throw new TrackUploadException(HttpStatus.INTERNAL_SERVER_ERROR, TrackErrorCode.TRACK_STORAGE_FAILED);
        }

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (InputStream input = file.getInputStream();
                 DigestInputStream digestInput = new DigestInputStream(input, digest);
                 OutputStream output = Files.newOutputStream(target)) {
                size = digestInput.transferTo(output);
            }

            return new StoredTrackFile(
                    STORAGE_PROVIDER,
                    storageKey,
                    size,
                    HexFormat.of().formatHex(digest.digest()),
                    target
            );
        } catch (IOException | NoSuchAlgorithmException ex) {
            delete(storageKey);
            throw new TrackUploadException(HttpStatus.INTERNAL_SERVER_ERROR, TrackErrorCode.TRACK_STORAGE_FAILED, ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        Path rootPath = storageProperties.getRootPath().toAbsolutePath().normalize();
        Path target = rootPath.resolve(storageKey).normalize();
        if (!target.startsWith(rootPath)) {
            log.warn("refusing to delete track file outside storage root");
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("failed to delete stored track file {}", storageKey, ex);
        }
    }

    private String buildStorageKey(String extension) {
        LocalDate date = LocalDate.now(clock);
        return "tracks/%04d/%02d/%s.%s".formatted(
                date.getYear(),
                date.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
    }
}
