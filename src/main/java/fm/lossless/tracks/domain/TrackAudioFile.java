package fm.lossless.tracks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
        name = "track_audio_files",
        indexes = {
                @Index(name = "ux_track_audio_files_track_id", columnList = "track_id", unique = true),
                @Index(name = "ux_track_audio_files_storage_key", columnList = "storage_key", unique = true)
        }
)
public class TrackAudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false, unique = true)
    private Track track;

    @NotBlank
    @Size(max = 32)
    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider;

    @NotBlank
    @Size(max = 512)
    @Column(name = "storage_key", nullable = false, length = 512, unique = true)
    private String storageKey;

    @Size(max = 255)
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Size(max = 255)
    @Column(length = 255)
    private String title;

    @Size(max = 255)
    @Column(name = "artist_name", length = 255)
    private String artistName;

    @Size(max = 255)
    @Column(name = "album_title", length = 255)
    private String albumTitle;

    @NotBlank
    @Size(max = 16)
    @Column(nullable = false, length = 16)
    private String extension;

    @Size(max = 120)
    @Column(name = "embedded_genre", length = 120)
    private String embeddedGenre;

    @NotNull
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @NotBlank
    @Size(max = 64)
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "sample_rate_hz")
    private Integer sampleRateHz;

    @Column(name = "bit_depth")
    private Integer bitDepth;

    @Column
    private Integer channels;

    @Column(name = "bitrate_kbps")
    private Integer bitrateKbps;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TrackAudioFile() {
    }

    public static TrackAudioFile create(
            Track track,
            String storageProvider,
            String storageKey,
            String originalFilename,
            String title,
            String artistName,
            String albumTitle,
            String extension,
            Long sizeBytes,
            String checksumSha256,
            String embeddedGenre,
            Integer durationSeconds,
            Integer sampleRateHz,
            Integer bitDepth,
            Integer channels,
            Integer bitrateKbps
    ) {
        TrackAudioFile file = new TrackAudioFile();
        file.track = track;
        file.storageProvider = storageProvider;
        file.storageKey = storageKey;
        file.originalFilename = originalFilename;
        file.title = title;
        file.artistName = artistName;
        file.albumTitle = albumTitle;
        file.extension = extension;
        file.sizeBytes = sizeBytes;
        file.checksumSha256 = checksumSha256;
        file.embeddedGenre = embeddedGenre;
        file.durationSeconds = durationSeconds;
        file.sampleRateHz = sampleRateHz;
        file.bitDepth = bitDepth;
        file.channels = channels;
        file.bitrateKbps = bitrateKbps;
        return file;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getStorageKey() {
        return storageKey;
    }
}
