package fm.lossless.tracks.web.dto;

import fm.lossless.tracks.domain.Genre;

public record GenreResponse(
        String slug,
        String name
) {
    public static GenreResponse from(Genre genre) {
        return new GenreResponse(genre.getSlug(), genre.getName());
    }
}
