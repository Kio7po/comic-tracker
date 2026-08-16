package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntryModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntryRequestDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntryResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.PageResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.ComicReadingEntryMapper;
import com.github.kio7po.comic_tracker.adapter.rest.security.CurrentUser;
import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingEntryService;
import com.github.kio7po.comic_tracker.domain.service.ComicService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api")
public class ComicReadingEntryController {

    private final ComicReadingEntryService comicReadingEntryService;
    private final ComicService comicService;

    public ComicReadingEntryController(ComicReadingEntryService comicReadingEntryService, ComicService comicService) {
        this.comicReadingEntryService = comicReadingEntryService;
        this.comicService = comicService;
    }

    @GetMapping("/comics/{slug}/reading-entries")
    public ResponseEntity<List<ComicReadingEntryResponseDto>> findByComic(@PathVariable String slug,
            @RequestParam(required = false) ComicReadingEntryStatus status) {
        return comicService.findBySlug(slug)
                .map(comic -> comicReadingEntryService.findByComic(comic.getId(), status))
                .map(ComicReadingEntryMapper::toResponseDtoList)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/comics/{slug}/reading-entries")
    public ResponseEntity<ComicReadingEntryResponseDto> submit(@PathVariable String slug,
            @Valid @RequestBody ComicReadingEntryRequestDto request, @CurrentUser Long contributorId) {
        return comicService.findBySlug(slug)
                .map(comic -> submit(comic, request, contributorId))
                .map(entry -> ResponseEntity.status(HttpStatus.CREATED).body(ComicReadingEntryMapper.toResponseDto(entry)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reading-entries")
    public PageResponseDto<ComicReadingEntryModerationResponseDto> findByStatusIn(
            @RequestParam List<ComicReadingEntryStatus> statuses,
            @RequestParam(defaultValue = "CREATED_AT") ComicReadingEntrySortField sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection direction,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        Page<ComicReadingEntry> page = comicReadingEntryService.findByStatusIn(statuses, sortBy, direction, limit,
                offset);
        return new PageResponseDto<>(ComicReadingEntryMapper.toModerationResponseDtoList(page.getItems()),
                page.isExistMoreItems(), page.getTotalItems());
    }

    @PostMapping("/reading-entries/{id}/approve")
    public ComicReadingEntryResponseDto approve(@PathVariable Long id, @CurrentUser Long reviewerId) {
        return ComicReadingEntryMapper.toResponseDto(comicReadingEntryService.approve(id, reviewerId));
    }

    @PostMapping("/reading-entries/{id}/reject")
    public ComicReadingEntryResponseDto reject(@PathVariable Long id, @CurrentUser Long reviewerId) {
        return ComicReadingEntryMapper.toResponseDto(comicReadingEntryService.reject(id, reviewerId));
    }

    private ComicReadingEntry submit(Comic comic, ComicReadingEntryRequestDto request, Long contributorId) {
        return request.sourceId() != null
                ? comicReadingEntryService.submit(comic.getId(), request.sourceId(), request.url(), request.locale(),
                        contributorId)
                : comicReadingEntryService.submitWithNewSource(comic.getId(), request.sourceName(),
                        request.sourceUrl(), request.url(), request.locale(), contributorId);
    }

}
