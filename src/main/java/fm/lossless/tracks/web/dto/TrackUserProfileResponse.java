package fm.lossless.tracks.web.dto;

import fm.lossless.users.domain.User;

public record TrackUserProfileResponse(
        Long id,
        String displayName
) {
    public static TrackUserProfileResponse from(User user) {
        return new TrackUserProfileResponse(user.getId(), user.getDisplayName());
    }
}
