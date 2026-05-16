package fm.lossless.tracks.service;

public record AudioMetadata(
        String title,
        String artistName,
        String albumTitle,
        String embeddedGenre,
        Integer durationSeconds,
        Integer sampleRateHz,
        Integer bitDepth,
        Integer channels,
        Integer bitrateKbps
) {
    public static AudioMetadata empty() {
        return new AudioMetadata(null, null, null, null, null, null, null, null, null);
    }
}
