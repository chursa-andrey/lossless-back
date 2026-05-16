package fm.lossless.tracks.service;

import fm.lossless.tracks.domain.Genre;
import fm.lossless.tracks.repo.GenreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Transactional(readOnly = true)
    public List<Genre> getActiveGenres() {
        return genreRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }
}
