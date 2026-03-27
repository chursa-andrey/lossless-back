package fm.lossless.auth.domain;

import fm.lossless.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "user_identities",
        indexes = {
                @Index(name = "ux_user_identities_provider_subject", columnList = "provider, provider_user_id", unique = true)
        }
)
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 32)
    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @NotBlank
    @Size(max = 255)
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Size(max = 320)
    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserIdentity() {
    }

    private UserIdentity(User user, String provider, String providerUserId, String email) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
    }

    public static UserIdentity create(User user, String provider, String providerUserId, String email) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(providerUserId, "providerUserId");
        return new UserIdentity(user, provider, providerUserId, email);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
