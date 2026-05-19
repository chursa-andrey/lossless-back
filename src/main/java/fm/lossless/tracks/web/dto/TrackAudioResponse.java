package fm.lossless.tracks.web.dto;

import fm.lossless.tracks.domain.TrackAudioFile;

public record TrackAudioResponse(
        String originalFilename,
        String extension,
        Long sizeBytes,
        Integer durationSeconds,
        Integer sampleRateHz,
        Integer bitDepth,
        Integer channels,
        Integer bitrateKbps,
        String embeddedGenre
) {
    public static TrackAudioResponse from(TrackAudioFile audioFile) {
        if (audioFile == null) {
            return null;
        }
        return new TrackAudioResponse(
                audioFile.getOriginalFilename(),
                audioFile.getExtension(),
                audioFile.getSizeBytes(),
                audioFile.getDurationSeconds(),
                audioFile.getSampleRateHz(),
                audioFile.getBitDepth(),
                audioFile.getChannels(),
                audioFile.getBitrateKbps(),
                audioFile.getEmbeddedGenre()
        );
    }
}
