package fm.lossless.auth.domain;

import fm.lossless.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_password_credentials")
public class UserPasswordCredential {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPasswordCredential() {
    }

    protected UserPasswordCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

    public static UserPasswordCredential create(User user, String passwordHash) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(passwordHash, "passwordHash");
        return new UserPasswordCredential(user, passwordHash);
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
