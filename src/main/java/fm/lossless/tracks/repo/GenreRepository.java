package fm.lossless.tracks.repo;

import fm.lossless.tracks.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    @Query("""
            select g
            from Genre g
            where g.active = true
              and (lower(g.slug) = lower(:value) or lower(g.name) = lower(:value))
            """)
    Optional<Genre> findActiveBySlugOrName(@Param("value") String value);

    List<Genre> findByActiveTrueOrderBySortOrderAscNameAsc();
}
