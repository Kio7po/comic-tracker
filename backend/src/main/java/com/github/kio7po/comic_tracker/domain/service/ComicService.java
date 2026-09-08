package com.github.kio7po.comic_tracker.domain.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;

// TODO: Considerar si tiene sentido que siga existiendo
@Service
public class ComicService {

    private final ComicRepository comicRepository;

    public ComicService(ComicRepository comicRepository) {
        this.comicRepository = comicRepository;
    }

    public Optional<Comic> findBySlug(String slug) {
        return comicRepository.findBySlug(slug);
    }

}
