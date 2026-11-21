package com.github.kio7po.comic_tracker.adapter.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntryProviderReference;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryProviderReferenceRepository;

public interface JpaComicReadingEntryProviderReferenceRepository
        extends JpaRepository<ComicReadingEntryProviderReference, Long>,
        ComicReadingEntryProviderReferenceRepository {

    // List<...> evita el choque de erasure con el saveAll(Iterable<S>) genérico de JpaRepository, así que
    // Spring Data no autoimplementa esta sobrecarga: se reenvía explícitamente a la real. El cast es lo que
    // la selecciona frente a este mismo método (un List es un Iterable, y sin él se llamaría a sí mismo).
    @Override
    default List<ComicReadingEntryProviderReference> saveAll(List<ComicReadingEntryProviderReference> references) {
        return saveAll((Iterable<ComicReadingEntryProviderReference>) references);
    }

}
