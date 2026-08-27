package com.github.kio7po.comic_tracker.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.entities.Author;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataSource;
import com.github.kio7po.comic_tracker.domain.entities.Genre;
import com.github.kio7po.comic_tracker.domain.entities.Tag;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicMetadataSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.UnsupportedMetadataSourceException;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataProvider;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;
import com.github.kio7po.comic_tracker.domain.port.persistence.AuthorRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicMetadataEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicMetadataSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.GenreRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.TagRepository;

@Service
public class CatalogService {

    private final ComicMetadataProvider metadataProvider;
    private final ComicRepository comicRepository;
    private final ComicMetadataEntryRepository comicMetadataEntryRepository;
    private final ComicMetadataSourceRepository comicMetadataSourceRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final ComicService comicService;
    private final Duration ttl;
    private final Duration retryBackoff;

    public CatalogService(ComicMetadataProvider metadataProvider, ComicRepository comicRepository,
            ComicMetadataEntryRepository comicMetadataEntryRepository,
            ComicMetadataSourceRepository comicMetadataSourceRepository, AuthorRepository authorRepository,
            GenreRepository genreRepository, TagRepository tagRepository, ComicService comicService,
            @Value("${comic.metadata.ttl}") Duration ttl,
            @Value("${comic.metadata.retry-backoff}") Duration retryBackoff) {
        this.metadataProvider = metadataProvider;
        this.comicRepository = comicRepository;
        this.comicMetadataEntryRepository = comicMetadataEntryRepository;
        this.comicMetadataSourceRepository = comicMetadataSourceRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.tagRepository = tagRepository;
        this.comicService = comicService;
        this.ttl = ttl;
        this.retryBackoff = retryBackoff;
    }

    public Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsfw,
            ComicStatus status, ComicMediaType type) {
        return metadataProvider.search(keywords, limit, offset, nsfw, status, type);
    }

    /*
     * TODO:
     * Dedup solo cubre el paso 1 de docs/TFG.md ("Proceso de alta de una fuente nueva"): reutilizar
     * el comic_id si (sourceSlug, externalId) ya está en ComicMetadataEntry. Los pasos 2-4
     * (referencia cruzada nativa, Wikidata) no aplican todavía porque solo hay un provider activo;
     * con un segundo provider, el mismo cómic buscado desde ambas fuentes generaría dos Comic
     * distintos hasta que se implementen.
     */
    @Transactional
    public Optional<Comic> importComic(String sourceSlug, String externalId) {
        if (!metadataProvider.getSourceSlug().equals(sourceSlug)) {
            throw new UnsupportedMetadataSourceException(sourceSlug);
        }

        ComicMetadataSource source = comicMetadataSourceRepository.findBySlug(sourceSlug)
                .orElseThrow(() -> new ComicMetadataSourceNotFoundException(sourceSlug));

        Optional<ComicMetadataEntry> existingEntry = comicMetadataEntryRepository.findBySourceAndExternalId(source,
                externalId);
        if (existingEntry.isPresent()) {
            ComicMetadataEntry entry = existingEntry.get();
            return Optional.of(refreshIfStale(entry.getComic(), entry));
        }

        return metadataProvider.fetch(externalId).map(result -> persistNewComic(result, source, externalId));
    }

    // TODO: Considerar si se deben actualizar los datos al obtener el cómic (cache-aside refres-on-read)
    // o solamente con import y con jobs
    @Transactional
    public Optional<Comic> getDetail(String slug) {
        return comicService.findBySlug(slug).map(this::refreshIfStale);
    }

    private Comic refreshIfStale(Comic comic) {
        return comicMetadataSourceRepository.findBySlug(metadataProvider.getSourceSlug())
                .flatMap(source -> comicMetadataEntryRepository.findByComicAndSource(comic, source))
                .map(entry -> refreshIfStale(comic, entry))
                .orElse(comic);
    }

    private Comic refreshIfStale(Comic comic, ComicMetadataEntry entry) {
        if (Instant.now().isBefore(entry.getLastFetchedAt().plus(ttl))) {
            return comic;
        }

        try {
            // TODO: Optional.empty() aquí significa que el proveedor ya no tiene el cómic (p. ej. borrado
            // en origen) - se trata igual que un refresco sin cambios (se actualiza lastFetchedAt, el
            // Comic local se deja tal cual). Merece quedar logueado cuando exista logging estructurado,
            // para poder detectar cómics huérfanos en origen sin tener que buscarlo activamente.
            metadataProvider.fetch(entry.getExternalId())
                    .ifPresent(result -> applyFetchedFields(comic, result.getComic()));
            entry.setLastFetchedAt(Instant.now());
        } catch (RuntimeException e) {
            // TODO: Control mas estricto de errores (errores de la capa de metadatos tipados)
            entry.setLastFetchedAt(Instant.now().minus(ttl).plus(retryBackoff));
        }
        comicMetadataEntryRepository.save(entry);

        return comicRepository.save(comic);
    }

    private Comic persistNewComic(ComicMetadataResult result, ComicMetadataSource source, String externalId) {
        Comic comic = result.getComic();
        comic.setAuthors(resolveAuthors(comic.getAuthors()));
        comic.setGenres(resolveGenres(comic.getGenres()));
        comic.setTags(resolveTags(comic.getTags()));
        comic.setSlug(generateSlug(comic.getTitle(), externalId));
        Comic savedComic = comicRepository.save(comic);

        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setExternalId(externalId);
        entry.setComic(savedComic);
        entry.setSource(source);
        entry.setLastFetchedAt(Instant.now());
        comicMetadataEntryRepository.save(entry);

        return savedComic;
    }

    // Applies every content field from a freshly fetched Comic onto an existing/target one -
    // deliberately never touches id/slug, so a refresh can never change a comic's stable URL.
    private void applyFetchedFields(Comic target, Comic fetched) {
        target.setTitle(fetched.getTitle());
        target.setSynopsis(fetched.getSynopsis());
        target.setCoverUrl(fetched.getCoverUrl());
        target.setAlternativeTitles(fetched.getAlternativeTitles());
        target.setStartDate(fetched.getStartDate());
        target.setEndDate(fetched.getEndDate());
        target.setNsfw(fetched.getNsfw());
        target.setMediaType(fetched.getMediaType());
        target.setStatus(fetched.getStatus());
        target.setChapters(fetched.getChapters());
        target.setAuthors(resolveAuthors(fetched.getAuthors()));
        target.setGenres(resolveGenres(fetched.getGenres()));
        target.setTags(resolveTags(fetched.getTags()));
    }

    private Set<Author> resolveAuthors(Set<Author> authors) {
        return authors.stream()
                .map(author -> authorRepository.findByName(author.getName())
                        .orElseGet(() -> authorRepository.save(author)))
                .collect(Collectors.toSet());
    }

    private Set<Genre> resolveGenres(Set<Genre> genres) {
        return genres.stream()
                .map(genre -> genreRepository.findByName(genre.getName())
                        .orElseGet(() -> genreRepository.save(genre)))
                .collect(Collectors.toSet());
    }

    private Set<Tag> resolveTags(Set<Tag> tags) {
        return tags.stream()
                .map(tag -> tagRepository.findByName(tag.getName()).orElseGet(() -> tagRepository.save(tag)))
                .collect(Collectors.toSet());
    }

    private String generateSlug(String title, String externalId) {
        String base = Slugifier.slugify(title);
        // Si el slug colisiona le añadimos el identificador externo
        /*
            TODO: externalId solo es único por fuente, no globalmente.
            Dos providers distintos podrían devolver el mismo externalId para cómics
            distintos cuyo título también colisione en el slug base, causando aquí una colisión real
            No se soluciona ahora; revisar cuando exista un segundo provider.
        */
        return comicRepository.findBySlug(base).isPresent() ? base + "-" + externalId : base;
    }

}
