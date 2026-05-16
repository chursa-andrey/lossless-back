package fm.lossless.tracks.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import fm.lossless.LosslessApplication;
import fm.lossless.tracks.repo.TrackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LosslessApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:trackuploadtest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "auth.jwt.issuer=test-issuer",
        "auth.jwt.secret=test-test-test-test-test-test-test-test",
        "app.storage.local.root-path=target/test-track-storage",
        "app.tracks.upload.max-file-size=10MB"
})
class TrackUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void uploadRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/tracks/upload")
                        .file(validWavFile())
                        .param("genre", "rock"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void genresRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tracks/genres"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void genresReturnsActiveGenresForAuthenticatedUser() throws Exception {
        String accessToken = registerAndGetAccessToken("track-genres@example.com");

        mockMvc.perform(get("/api/v1/tracks/genres")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("rock"))
                .andExpect(jsonPath("$[0].name").value("Rock"));
    }

    @Test
    void uploadCreatesTrackForAuthenticatedUser() throws Exception {
        String accessToken = registerAndGetAccessToken("track-upload@example.com");

        String response = mockMvc.perform(multipart("/api/v1/tracks/upload")
                        .file(validWavFile())
                        .param("genre", "rock")
                        .param("title", "Form Title")
                        .param("artistName", "Form Artist")
                        .param("albumTitle", "Form Album")
                        .param("purchaseLinks[]", "https://example.com/buy")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long trackId = objectMapper.readTree(response).path("trackId").asLong();
        assertThat(trackRepository.findById(trackId)).isPresent();
    }

    @Test
    void uploadDecodesUrlEncodedOriginalFilename() throws Exception {
        String accessToken = registerAndGetAccessToken("track-encoded-filename@example.com");

        String response = mockMvc.perform(multipart("/api/v1/tracks/upload")
                        .file(encodedFlacFile())
                        .param("genre", "rock")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long trackId = objectMapper.readTree(response).path("trackId").asLong();
        String originalFilename = jdbcTemplate.queryForObject(
                "select original_filename from track_audio_files where track_id = ?",
                String.class,
                trackId
        );

        assertThat(originalFilename).isEqualTo("(20) [Наутилус Помпилиус] Крылья.flac");
    }

    @Test
    void deletingTrackCascadesToAudioFile() throws Exception {
        String accessToken = registerAndGetAccessToken("track-audio-cascade@example.com");

        String response = mockMvc.perform(multipart("/api/v1/tracks/upload")
                        .file(validWavFile())
                        .param("genre", "rock")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long trackId = objectMapper.readTree(response).path("trackId").asLong();
        Integer audioFileCountBeforeDelete = jdbcTemplate.queryForObject(
                "select count(*) from track_audio_files where track_id = ?",
                Integer.class,
                trackId
        );

        jdbcTemplate.update("delete from tracks where id = ?", trackId);

        Integer audioFileCountAfterDelete = jdbcTemplate.queryForObject(
                "select count(*) from track_audio_files where track_id = ?",
                Integer.class,
                trackId
        );

        assertThat(audioFileCountBeforeDelete).isEqualTo(1);
        assertThat(audioFileCountAfterDelete).isZero();
    }


    @Test
    void uploadRejectsUnknownGenreWithStableCode() throws Exception {
        String accessToken = registerAndGetAccessToken("track-bad-genre@example.com");

        mockMvc.perform(multipart("/api/v1/tracks/upload")
                        .file(validWavFile())
                        .param("genre", "unknown-genre")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRACK_GENRE_NOT_FOUND"));
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Passw0rd!",
                  "displayName": "Track User"
                }
                """.formatted(email);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("accessToken").asText();
    }

    private MockMultipartFile validWavFile() {
        byte[] bytes = {
                'R', 'I', 'F', 'F',
                0, 0, 0, 0,
                'W', 'A', 'V', 'E',
                'f', 'm', 't', ' '
        };
        return new MockMultipartFile("file", "track.wav", "audio/wav", bytes);
    }

    private MockMultipartFile encodedFlacFile() {
        byte[] bytes = {
                'f', 'L', 'a', 'C',
                0, 0, 0, 0
        };
        return new MockMultipartFile(
                "file",
                "(20)%20%5B%D0%9D%D0%B0%D1%83%D1%82%D0%B8%D0%BB%D1%83%D1%81%20%D0%9F%D0%BE%D0%BC%D0%BF%D0%B8%D0%BB%D0%B8%D1%83%D1%81%5D%20%D0%9A%D1%80%D1%8B%D0%BB%D1%8C%D1%8F.flac",
                "audio/flac",
                bytes
        );
    }
}
