package fm.lossless.users.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void createUsesProvidedDisplayNameWhenPresent() {
        User user = User.create("user@example.com", "  John Doe  ");

        assertThat(user.getDisplayName()).isEqualTo("John Doe");
    }

    @Test
    void createFallsBackToEmailLocalPart() {
        User user = User.create("local.part@example.com", null);

        assertThat(user.getDisplayName()).isEqualTo("local.part");
    }

    @Test
    void createFallsBackToDefaultWhenEmailLocalPartIsEmpty() {
        User user = User.create("@example.com", "   ");

        assertThat(user.getDisplayName()).isEqualTo("user");
    }

    @Test
    void createTruncatesDisplayNameToMaxLength() {
        String longName = "x".repeat(120);

        User user = User.create("user@example.com", longName);

        assertThat(user.getDisplayName()).hasSize(100);
    }
}
