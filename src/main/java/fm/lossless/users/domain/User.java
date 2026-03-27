package fm.lossless.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "ux_users_email", columnList = "email", unique = true)
        }
)
public class User {

    private static final String DEFAULT_DISPLAY_NAME = "user";
    private static final int MAX_DISPLAY_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 320)
    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @NotBlank
    @Size(max = 100)
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false)
    )
    private Set<Role> roles = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected User() { }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static User create(String email, String displayName) {
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(resolveDisplayName(displayName, email));
        return u;
    }

    private static String resolveDisplayName(String rawDisplayName, String email) {
        if (rawDisplayName != null && !rawDisplayName.isBlank()) {
            return truncateDisplayName(rawDisplayName.trim());
        }

        String normalizedEmail = normalizeEmail(email);
        int atIndex = normalizedEmail == null ? -1 : normalizedEmail.indexOf('@');
        String localPart;
        if (atIndex >= 0) {
            localPart = normalizedEmail.substring(0, atIndex);
        } else {
            localPart = normalizedEmail == null ? "" : normalizedEmail;
        }
        if (localPart.isBlank()) {
            return DEFAULT_DISPLAY_NAME;
        }

        return truncateDisplayName(localPart);
    }

    private static String truncateDisplayName(String source) {
        return source.length() <= MAX_DISPLAY_NAME_LENGTH
                ? source
                : source.substring(0, MAX_DISPLAY_NAME_LENGTH);
    }

    @PrePersist
    void onCreate() {
        normalizeEmailInternal();
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
        normalizeEmailInternal();
        updatedAt = Instant.now();
    }

    private void normalizeEmailInternal() {
        this.email = normalizeEmail(this.email);
    }

    public Long getId() { return id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = normalizeEmail(email); }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        if (role != null) {
            roles.add(role);
        }
    }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', displayName='" + displayName + "'}";
    }
}
