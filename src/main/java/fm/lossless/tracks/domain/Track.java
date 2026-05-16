package fm.lossless.tracks.domain;

import fm.lossless.users.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "tracks",
        indexes = {
                @Index(name = "ix_tracks_genre_id", columnList = "genre_id"),
                @Index(name = "ix_tracks_created_by_user_id", columnList = "created_by_user_id"),
                @Index(name = "ix_tracks_status_created_at", columnList = "status, created_at")
        }
)
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Size(max = 255)
    @Column(length = 255)
    private String title;

    @Size(max = 255)
    @Column(name = "artist_name", length = 255)
    private String artistName;

    @Size(max = 255)
    @Column(name = "album_title", length = 255)
    private String albumTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TrackStatus status = TrackStatus.UPLOADED;

    @OneToOne(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TrackAudioFile audioFile;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackPurchaseLink> purchaseLinks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Track() {
    }

    public static Track create(Genre genre, User createdBy, String title, String artistName, String albumTitle) {
        Track track = new Track();
        track.genre = genre;
        track.createdBy = createdBy;
        track.title = title;
        track.artistName = artistName;
        track.albumTitle = albumTitle;
        track.status = TrackStatus.UPLOADED;
        return track;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void setAudioFile(TrackAudioFile audioFile) {
        this.audioFile = audioFile;
    }

    public void addPurchaseLink(TrackPurchaseLink purchaseLink) {
        purchaseLinks.add(purchaseLink);
    }

    public Long getId() {
        return id;
    }

    public Genre getGenre() {
        return genre;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getTitle() {
        return title;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public TrackStatus getStatus() {
        return status;
    }

    public TrackAudioFile getAudioFile() {
        return audioFile;
    }

    public List<TrackPurchaseLink> getPurchaseLinks() {
        return purchaseLinks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
