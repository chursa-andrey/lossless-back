package fm.lossless.tracks.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "app.storage.local")
@Validated
public class StorageProperties {

    @NotNull
    private Path rootPath = Path.of("./storage");

    public Path getRootPath() {
        return rootPath;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }
}
