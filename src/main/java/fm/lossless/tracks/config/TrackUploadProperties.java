package fm.lossless.tracks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "app.tracks.upload")
public class TrackUploadProperties {

    private DataSize maxFileSize = DataSize.ofMegabytes(500);
    private List<String> allowedExtensions = new ArrayList<>(List.of("wav", "flac"));
    private int maxPurchaseLinks = 10;

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions == null
                ? new ArrayList<>()
                : allowedExtensions.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.trim().toLowerCase(Locale.ROOT))
                        .toList();
    }

    public int getMaxPurchaseLinks() {
        return maxPurchaseLinks;
    }

    public void setMaxPurchaseLinks(int maxPurchaseLinks) {
        this.maxPurchaseLinks = maxPurchaseLinks;
    }
}
