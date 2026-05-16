package fm.lossless.tracks.service;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.nio.file.Path;

@Service
public class AudioMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(AudioMetadataExtractor.class);

    public AudioMetadata extract(Path filePath) {
        if (filePath == null) {
            return AudioMetadata.empty();
        }

        try {
            AudioFile audioFile = AudioFileIO.read(filePath.toFile());
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();

            return new AudioMetadata(
                    readTag(tag, FieldKey.TITLE),
                    readTag(tag, FieldKey.ARTIST),
                    readTag(tag, FieldKey.ALBUM),
                    readTag(tag, FieldKey.GENRE),
                    positiveOrNull(header == null ? null : header.getTrackLength()),
                    positiveOrNull(header == null ? null : header.getSampleRateAsNumber()),
                    positiveOrNull(invokeIntegerMethod(header, "getBitsPerSample")),
                    parsePositiveInt(header == null ? null : header.getChannels()),
                    positiveOrNull(header == null ? null : header.getBitRateAsNumber())
            );
        } catch (Exception ex) {
            log.debug("audio metadata extraction failed for {}", filePath.getFileName(), ex);
            return AudioMetadata.empty();
        }
    }

    private String readTag(Tag tag, FieldKey fieldKey) {
        if (tag == null) {
            return null;
        }
        return trimToNull(tag.getFirst(fieldKey));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer positiveOrNull(Number value) {
        if (value == null || value.longValue() <= 0) {
            return null;
        }
        long longValue = value.longValue();
        return longValue > Integer.MAX_VALUE ? null : (int) longValue;
    }

    private Integer parsePositiveInt(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        try {
            return positiveOrNull(Integer.parseInt(trimmed));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer invokeIntegerMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String stringValue) {
                return parsePositiveInt(stringValue);
            }
            return null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
