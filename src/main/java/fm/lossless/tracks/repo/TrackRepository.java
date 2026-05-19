package fm.lossless.tracks.repo;

import fm.lossless.tracks.domain.Track;
import fm.lossless.tracks.domain.TrackStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    @Query("""
            select t.id
            from Track t
            where t.status = :status
            order by t.createdAt desc, t.id desc
            """)
    List<Long> findInitialFeedIds(
            @Param("status") TrackStatus status,
            Pageable pageable
    );

    @Query("""
            select t.id
            from Track t
            where t.status = :status
              and (
                t.createdAt < :cursorCreatedAt
                or (t.createdAt = :cursorCreatedAt and t.id < :cursorId)
              )
            order by t.createdAt desc, t.id desc
            """)
    List<Long> findFeedIdsAfterCursor(
            @Param("status") TrackStatus status,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"genre", "createdBy", "audioFile", "purchaseLinks"})
    @Query("select distinct t from Track t where t.id in :ids")
    List<Track> findFeedItemsByIds(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"audioFile"})
    Optional<Track> findWithAudioFileById(Long id);
}
