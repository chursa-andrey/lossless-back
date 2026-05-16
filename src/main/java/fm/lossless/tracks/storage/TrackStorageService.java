package fm.lossless.tracks.storage;

import org.springframework.web.multipart.MultipartFile;

public interface TrackStorageService {

    StoredTrackFile store(MultipartFile file, String extension);

    void delete(String storageKey);
}
