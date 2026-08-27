package com.github.kio7po.comic_tracker.adapter.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.CatalogMapper;
import com.github.kio7po.comic_tracker.domain.service.CatalogService;

@RestController
@RequestMapping("/api/comics")
public class ComicController {

    private final CatalogService catalogService;

    public ComicController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ComicResponseDto> getBySlug(@PathVariable String slug) {
        return catalogService.getDetail(slug)
                .map(CatalogMapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
