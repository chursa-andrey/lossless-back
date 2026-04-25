package fm.lossless.auth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fm.lossless.LosslessApplication;
import fm.lossless.auth.repo.UserIdentityRepository;
import fm.lossless.auth.service.social.GoogleSocialTokenVerifier;
import fm.lossless.auth.service.social.SocialProvider;
import fm.lossless.auth.service.social.SocialProviderProfile;
import fm.lossless.users.domain.User;
import fm.lossless.users.repo.RoleRepository;
import fm.lossless.users.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LosslessApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "auth.jwt.issuer=test-issuer",
        "auth.jwt.secret=test-test-test-test-test-test-test-test"
})
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @SpyBean
    private GoogleSocialTokenVerifier googleSocialTokenVerifier;

    @Test
    void registerThenReadCurrentUser() throws Exception {
        String registerBody = """
                {
                  "email": "user@example.com",
                  "password": "Passw0rd!",
                  "displayName": "User"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerResponse);
        String accessToken = registerJson.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void refreshRotatesToken() throws Exception {
        String registerBody = """
                {
                  "email": "rotate@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Rotate"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(registerResponse).path("refreshToken").asText();

        String refreshBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newRefreshToken = objectMapper.readTree(refreshResponse).path("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRejectsUnknownToken() throws Exception {
        String unknownToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String refreshBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(unknownToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRejectsInvalidRequestWithValidationCode() throws Exception {
        String body = """
                {
                  "refreshToken": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String registerBody = """
                {
                  "email": "logout@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Logout"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(registerResponse).path("refreshToken").asText();

        String body = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutIsIdempotentForUnknownRefreshToken() throws Exception {
        String body = """
                {
                  "refreshToken": "unknown-refresh-token-with-valid-request-length"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        String registerBody = """
                {
                  "email": "bad-login@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Bad Login"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        String loginBody = """
                {
                  "email": "bad-login@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String registerBody = """
                {
                  "email": "duplicate@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Dup"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void continueCreatesAccountWhenMissing() throws Exception {
        String continueBody = """
                {
                  "email": "continue-new@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Continue New"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(continueBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("continue-new@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void continueLogsInExistingUserWhenPasswordMatches() throws Exception {
        String registerBody = """
                {
                  "email": "continue-existing@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Continue Existing"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        String continueBody = """
                {
                  "email": "continue-existing@example.com",
                  "password": "Passw0rd!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(continueBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("continue-existing@example.com"));
    }

    @Test
    void continueRejectsWrongPasswordWithInvalidCredentialsCode() throws Exception {
        String registerBody = """
                {
                  "email": "continue-wrong-password@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Continue Wrong Password"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        String continueBody = """
                {
                  "email": "continue-wrong-password@example.com",
                  "password": "WrongPass1!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(continueBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void continueRejectsPasswordlessAccountWithDedicatedCode() throws Exception {
        createPasswordlessUser("continue-passwordless@example.com", "Passwordless");

        String continueBody = """
                {
                  "email": "continue-passwordless@example.com",
                  "password": "Passw0rd!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(continueBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PASSWORD_LOGIN_NOT_AVAILABLE"));
    }

    @Test
    void socialRejectsUnsupportedProviderWithDedicatedCode() throws Exception {
        String body = """
                {
                  "providerToken": "token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/social/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_SOCIAL_PROVIDER"));
    }

    @Test
    void socialRejectsInvalidRequestWithValidationCode() throws Exception {
        String body = """
                {
                  "providerToken": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void socialCreatesUserIdentityAndIssuesTokens() throws Exception {
        doReturn(new SocialProviderProfile(
                SocialProvider.GOOGLE,
                "google-create-subject",
                "social-create@example.com",
                true,
                "Social Create"
        )).when(googleSocialTokenVerifier).verify("google-create-token");

        String body = """
                {
                  "providerToken": "google-create-token"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("social-create@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Social Create"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        String accessToken = responseJson.path("accessToken").asText();

        User user = userRepository.findByEmail("social-create@example.com").orElseThrow();
        assertThat(userIdentityRepository.findByProviderAndProviderUserId("google", "google-create-subject"))
                .isPresent()
                .get()
                .extracting(identity -> identity.getUser().getId())
                .isEqualTo(user.getId());

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("social-create@example.com"));
    }

    @Test
    void socialLinksExistingUserByVerifiedEmail() throws Exception {
        String registerBody = """
                {
                  "email": "social-link@example.com",
                  "password": "Passw0rd!",
                  "displayName": "Existing User"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long existingUserId = objectMapper.readTree(registerResponse).path("user").path("id").asLong();

        doReturn(new SocialProviderProfile(
                SocialProvider.GOOGLE,
                "google-link-subject",
                "SOCIAL-LINK@example.com",
                true,
                "Ignored Social Name"
        )).when(googleSocialTokenVerifier).verify("google-link-token");

        String socialBody = """
                {
                  "providerToken": "google-link-token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(socialBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(existingUserId))
                .andExpect(jsonPath("$.user.email").value("social-link@example.com"));

        assertThat(userIdentityRepository.findByProviderAndProviderUserId("google", "google-link-subject"))
                .isPresent()
                .get()
                .extracting(identity -> identity.getUser().getId())
                .isEqualTo(existingUserId);
        assertThat(userRepository.findByEmail("social-link@example.com")).isPresent();
    }

    @Test
    void meEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private void createPasswordlessUser(String email, String displayName) {
        User user = User.create(email, displayName);
        user.addRole(roleRepository.findByCode("USER").orElseThrow());
        userRepository.saveAndFlush(user);
    }
}
