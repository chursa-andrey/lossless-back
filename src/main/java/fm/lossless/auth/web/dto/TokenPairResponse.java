package fm.lossless.auth.web.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken
) {
}
