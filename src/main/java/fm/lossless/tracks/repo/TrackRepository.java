package fm.lossless.tracks.repo;

import fm.lossless.tracks.domain.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
}
