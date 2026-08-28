package com.github.kio7po.comic_tracker.adapter.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicSearchResultResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.PageResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.CatalogMapper;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicSearchSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.service.CatalogService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/search")
    public PageResponseDto<ComicSearchResultResponseDto> search(
            @RequestParam String keywords,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(required = false) NsfwRating nsfw,
            @RequestParam(required = false) ComicStatus status,
            @RequestParam(required = false) ComicMediaType type,
            @RequestParam(required = false) ComicSearchSortField sortBy,
            @RequestParam(required = false) SortDirection direction) {
        return CatalogMapper.toSearchResultPageDto(
                catalogService.search(keywords, limit, offset, nsfw, status, type, sortBy, direction));
    }

    @PostMapping("/{sourceSlug}/{externalId}")
    public ResponseEntity<ComicResponseDto> importComic(@PathVariable String sourceSlug,
            @PathVariable String externalId) {
        return catalogService.importComic(sourceSlug, externalId)
                .map(CatalogMapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
