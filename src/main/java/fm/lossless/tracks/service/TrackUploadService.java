package fm.lossless.tracks.service;

import fm.lossless.auth.security.AuthPrincipal;
import fm.lossless.tracks.config.TrackUploadProperties;
import fm.lossless.tracks.domain.Genre;
import fm.lossless.tracks.domain.Track;
import fm.lossless.tracks.domain.TrackAudioFile;
import fm.lossless.tracks.domain.TrackPurchaseLink;
import fm.lossless.tracks.exception.TrackErrorCode;
import fm.lossless.tracks.exception.TrackUploadException;
import fm.lossless.tracks.repo.GenreRepository;
import fm.lossless.tracks.repo.TrackRepository;
import fm.lossless.tracks.storage.StoredTrackFile;
import fm.lossless.tracks.storage.TrackStorageService;
import fm.lossless.users.domain.User;
import fm.lossless.users.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TrackUploadService {

    private static final int MAX_TEXT_LENGTH = 255;
    private static final int MAX_ORIGINAL_FILENAME_LENGTH = 255;

    private final TrackUploadProperties uploadProperties;
    private final CurrentUserService currentUserService;
    private final GenreRepository genreRepository;
    private final TrackRepository trackRepository;
    private final TrackStorageService storageService;
    private final AudioMetadataExtractor metadataExtractor;

    public TrackUploadService(
            TrackUploadProperties uploadProperties,
            CurrentUserService currentUserService,
            GenreRepository genreRepository,
            TrackRepository trackRepository,
            TrackStorageService storageService,
            AudioMetadataExtractor metadataExtractor
    ) {
        this.uploadProperties = uploadProperties;
        this.currentUserService = currentUserService;
        this.genreRepository = genreRepository;
        this.trackRepository = trackRepository;
        this.storageService = storageService;
        this.metadataExtractor = metadataExtractor;
    }

    @Transactional
    public Long upload(
            AuthPrincipal principal,
            MultipartFile file,
            String genreValue,
            String title,
            String artistName,
            String albumTitle,
            List<String> purchaseLinks
    ) {
        User currentUser = currentUserService.getCurrentUser(principal);
        ValidatedUploadInput input = validateInput(file, genreValue, title, artistName, albumTitle, purchaseLinks);
        Genre genre = genreRepository.findActiveBySlugOrName(input.genre())
                .orElseThrow(() -> new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                        TrackErrorCode.TRACK_GENRE_NOT_FOUND));

        StoredTrackFile storedFile = storageService.store(file, input.extension());
        deleteStoredFileOnRollback(storedFile.storageKey());

        AudioMetadata metadata = metadataExtractor.extract(storedFile.metadataSourcePath());
        Track track = Track.create(
                genre,
                currentUser,
                preferMetadata(metadata.title(), input.title()),
                preferMetadata(metadata.artistName(), input.artistName()),
                preferMetadata(metadata.albumTitle(), input.albumTitle())
        );
        TrackAudioFile audioFile = TrackAudioFile.create(
                track,
                storedFile.storageProvider(),
                storedFile.storageKey(),
                input.originalFilename(),
                trimText(metadata.title()),
                trimText(metadata.artistName()),
                trimText(metadata.albumTitle()),
                input.extension(),
                storedFile.sizeBytes(),
                storedFile.checksumSha256(),
                metadata.embeddedGenre(),
                metadata.durationSeconds(),
                metadata.sampleRateHz(),
                metadata.bitDepth(),
                metadata.channels(),
                metadata.bitrateKbps()
        );
        track.setAudioFile(audioFile);

        int position = 0;
        for (String url : input.purchaseLinks()) {
            track.addPurchaseLink(TrackPurchaseLink.create(track, url, position++));
        }

        Track saved = trackRepository.saveAndFlush(track);
        return saved.getId();
    }

    private ValidatedUploadInput validateInput(
            MultipartFile file,
            String genre,
            String title,
            String artistName,
            String albumTitle,
            List<String> purchaseLinks
    ) {
        validateFileIsPresent(file);
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractAllowedExtension(originalFilename);
        validateFileSize(file);
        validateContentType(file.getContentType(), extension);
        validateDetectedFormat(file, extension);

        String normalizedGenre = trimToNull(genre);
        if (normalizedGenre == null) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY, TrackErrorCode.TRACK_GENRE_REQUIRED);
        }

        return new ValidatedUploadInput(
                extension,
                normalizedGenre,
                trimText(title),
                trimText(artistName),
                trimText(albumTitle),
                normalizePurchaseLinks(purchaseLinks),
                originalFilename
        );
    }

    private void validateFileIsPresent(MultipartFile file) {
        if (file == null) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY, TrackErrorCode.TRACK_FILE_REQUIRED);
        }
        if (file.isEmpty()) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY, TrackErrorCode.TRACK_FILE_EMPTY);
        }
    }

    private String extractAllowedExtension(String originalFilename) {
        int dotIndex = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        String extension = dotIndex < 0 ? null : originalFilename.substring(dotIndex + 1);
        extension = extension == null ? null : extension.trim().toLowerCase(Locale.ROOT);

        if (extension == null || extension.isBlank() || !uploadProperties.getAllowedExtensions().contains(extension)) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    TrackErrorCode.TRACK_FILE_EXTENSION_NOT_ALLOWED);
        }
        return extension;
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > uploadProperties.getMaxFileSize().toBytes()) {
            throw new TrackUploadException(HttpStatus.PAYLOAD_TOO_LARGE, TrackErrorCode.TRACK_FILE_TOO_LARGE);
        }
    }

    private void validateContentType(String contentType, String extension) {
        String normalized = trimToNull(contentType);
        if (normalized == null) {
            return;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if ("application/octet-stream".equals(normalized)) {
            return;
        }

        boolean compatible = switch (extension) {
            case "wav" -> normalized.equals("audio/wav")
                    || normalized.equals("audio/wave")
                    || normalized.equals("audio/x-wav")
                    || normalized.equals("audio/vnd.wave");
            case "flac" -> normalized.equals("audio/flac")
                    || normalized.equals("audio/x-flac")
                    || normalized.equals("application/flac");
            default -> false;
        };

        if (!compatible) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    TrackErrorCode.TRACK_FILE_CONTENT_TYPE_NOT_ALLOWED);
        }
    }

    private void validateDetectedFormat(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        int bytesRead;
        try (InputStream input = file.getInputStream()) {
            bytesRead = input.read(header);
        } catch (IOException ex) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    TrackErrorCode.TRACK_FILE_FORMAT_NOT_ALLOWED, ex);
        }

        boolean valid = switch (extension) {
            case "wav" -> bytesRead >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'A'
                    && header[10] == 'V'
                    && header[11] == 'E';
            case "flac" -> bytesRead >= 4
                    && header[0] == 'f'
                    && header[1] == 'L'
                    && header[2] == 'a'
                    && header[3] == 'C';
            default -> false;
        };

        if (!valid) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    TrackErrorCode.TRACK_FILE_FORMAT_NOT_ALLOWED);
        }
    }

    private List<String> normalizePurchaseLinks(List<String> purchaseLinks) {
        if (purchaseLinks == null || purchaseLinks.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String purchaseLink : purchaseLinks) {
            String value = trimToNull(purchaseLink);
            if (value == null) {
                continue;
            }
            validatePurchaseLink(value);
            normalized.add(value);
        }

        if (normalized.size() > uploadProperties.getMaxPurchaseLinks()) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    TrackErrorCode.TRACK_PURCHASE_LINK_LIMIT_EXCEEDED);
        }

        return List.copyOf(normalized);
    }

    private void validatePurchaseLink(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                        TrackErrorCode.TRACK_PURCHASE_LINK_INVALID);
            }
        } catch (URISyntaxException ex) {
            throw new TrackUploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    TrackErrorCode.TRACK_PURCHASE_LINK_INVALID, ex);
        }
    }

    private String preferMetadata(String metadataValue, String formValue) {
        String normalizedMetadata = trimToNull(metadataValue);
        return normalizedMetadata != null ? trimText(normalizedMetadata) : formValue;
    }

    private String trimText(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() > MAX_TEXT_LENGTH ? trimmed.substring(0, MAX_TEXT_LENGTH) : trimmed;
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        String value = trimToNull(originalFilename);
        if (value == null) {
            return null;
        }
        value = decodeUrlEncodedFilenameIfNeeded(value);
        value = value.replace('\\', '/');
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0) {
            value = value.substring(slashIndex + 1);
        }
        return value.length() > MAX_ORIGINAL_FILENAME_LENGTH
                ? value.substring(value.length() - MAX_ORIGINAL_FILENAME_LENGTH)
                : value;
    }

    private String decodeUrlEncodedFilenameIfNeeded(String value) {
        if (!hasPercentEncodedByte(value)) {
            return value;
        }

        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private boolean hasPercentEncodedByte(String value) {
        for (int index = 0; index < value.length() - 2; index++) {
            if (value.charAt(index) == '%'
                    && isHexDigit(value.charAt(index + 1))
                    && isHexDigit(value.charAt(index + 2))) {
                return true;
            }
        }
        return false;
    }

    private boolean isHexDigit(char value) {
        return (value >= '0' && value <= '9')
                || (value >= 'a' && value <= 'f')
                || (value >= 'A' && value <= 'F');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void deleteStoredFileOnRollback(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    storageService.delete(storageKey);
                }
            }
        });
    }

    private record ValidatedUploadInput(
            String extension,
            String genre,
            String title,
            String artistName,
            String albumTitle,
            List<String> purchaseLinks,
            String originalFilename
    ) {
    }
}
