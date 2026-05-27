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
import static org.hamcrest.Matchers.hasSize;
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
        "app.tracks.feed.default-page-size=2",
        "app.tracks.feed.max-page-size=3",
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
    void feedRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tracks"))
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
    void feedReturnsNewestTracksFirstWithUploaderAndPlaybackUrl() throws Exception {
        deleteTracks();
        String accessToken = registerAndGetAccessToken("track-feed-owner@example.com", "Feed Owner");

        Long firstTrackId = uploadTrack(accessToken, "First Track");
        Long secondTrackId = uploadTrack(accessToken, "Second Track");
        Long thirdTrackId = uploadTrack(accessToken, "Third Track");

        String firstPage = mockMvc.perform(get("/api/v1/tracks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(thirdTrackId))
                .andExpect(jsonPath("$.items[0].title").value("Third Track"))
                .andExpect(jsonPath("$.items[0].genre.slug").value("rock"))
                .andExpect(jsonPath("$.items[0].uploadedBy.displayName").value("Feed Owner"))
                .andExpect(jsonPath("$.items[0].uploadedBy.email").doesNotExist())
                .andExpect(jsonPath("$.items[0].audioUrl").value("/api/v1/tracks/" + thirdTrackId + "/audio"))
                .andExpect(jsonPath("$.items[0].audio.extension").value("wav"))
                .andExpect(jsonPath("$.items[0].purchaseLinks[0].url").value("https://example.com/first"))
                .andExpect(jsonPath("$.items[1].id").value(secondTrackId))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor.id").value(secondTrackId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String cursorCreatedAt = objectMapper.readTree(firstPage).path("nextCursor").path("createdAt").asText();
        String cursorId = objectMapper.readTree(firstPage).path("nextCursor").path("id").asText();

        mockMvc.perform(get("/api/v1/tracks")
                        .param("cursorCreatedAt", cursorCreatedAt)
                        .param("cursorId", cursorId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(firstTrackId))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void feedRejectsIncompleteCursor() throws Exception {
        String accessToken = registerAndGetAccessToken("track-feed-cursor@example.com");

        mockMvc.perform(get("/api/v1/tracks")
                        .param("cursorId", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRACK_FEED_CURSOR_INVALID"));
    }

    @Test
    void trackAudioReturnsStoredFile() throws Exception {
        String accessToken = registerAndGetAccessToken("track-audio-playback@example.com");
        Long trackId = uploadTrack(accessToken, "Playable Track");

        byte[] response = mockMvc.perform(get("/api/v1/tracks/{trackId}/audio", trackId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(response).startsWith(new byte[]{'R', 'I', 'F', 'F'});
    }

    @Test
    void trackAudioSupportsSingleByteRange() throws Exception {
        String accessToken = registerAndGetAccessToken("track-audio-range@example.com");
        Long trackId = uploadTrack(accessToken, "Range Track");

        byte[] response = mockMvc.perform(get("/api/v1/tracks/{trackId}/audio", trackId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Range", "bytes=0-3"))
                .andExpect(status().isPartialContent())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(response).containsExactly('R', 'I', 'F', 'F');
    }

    @Test
    void trackAudioRejectsInvalidRange() throws Exception {
        String accessToken = registerAndGetAccessToken("track-audio-invalid-range@example.com");
        Long trackId = uploadTrack(accessToken, "Invalid Range Track");

        mockMvc.perform(get("/api/v1/tracks/{trackId}/audio", trackId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Range", "bytes=100-200"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(jsonPath("$.code").value("TRACK_AUDIO_RANGE_INVALID"));
    }

    @Test
    void feedRejectsMalformedCursorTypeWithStableCode() throws Exception {
        String accessToken = registerAndGetAccessToken("track-feed-malformed-cursor@example.com");

        mockMvc.perform(get("/api/v1/tracks")
                        .param("cursorCreatedAt", "not-an-instant")
                        .param("cursorId", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRACK_FEED_CURSOR_INVALID"));
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
        return registerAndGetAccessToken(email, "Track User");
    }

    private String registerAndGetAccessToken(String email, String displayName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Passw0rd!",
                  "displayName": "%s"
                }
                """.formatted(email, displayName);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("accessToken").asText();
    }

    private Long uploadTrack(String accessToken, String title) throws Exception {
        String response = mockMvc.perform(multipart("/api/v1/tracks/upload")
                        .file(validWavFile())
                        .param("genre", "rock")
                        .param("title", title)
                        .param("purchaseLinks[]", "https://example.com/first")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("trackId").asLong();
    }

    private void deleteTracks() {
        jdbcTemplate.update("delete from tracks");
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
