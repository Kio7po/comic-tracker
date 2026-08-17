package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.ComicReadingEntryMapper;
import com.github.kio7po.comic_tracker.adapter.rest.security.CurrentUser;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceService;

@RestController
@RequestMapping("/api")
public class ComicReadingSourceController {

    private final ComicReadingSourceService comicReadingSourceService;

    public ComicReadingSourceController(ComicReadingSourceService comicReadingSourceService) {
        this.comicReadingSourceService = comicReadingSourceService;
    }

    @GetMapping("/reading-sources")
    public List<ComicReadingSourceResponseDto> findByStatusIn(@RequestParam List<ComicReadingSourceStatus> statuses,
            @RequestParam(defaultValue = "NAME") ComicReadingSourceSortField sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return ComicReadingEntryMapper
                .toSourceResponseDtoList(comicReadingSourceService.findByStatusIn(statuses, sortBy, direction));
    }

    @GetMapping("/moderation/reading-sources")
    public List<ComicReadingSourceModerationResponseDto> findForModerationByStatusIn(
            @RequestParam List<ComicReadingSourceStatus> statuses,
            @RequestParam(defaultValue = "NAME") ComicReadingSourceSortField sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return ComicReadingEntryMapper
                .toSourceModerationResponseDtoList(comicReadingSourceService.findByStatusIn(statuses, sortBy, direction));
    }

    @PostMapping("/reading-sources/{id}/approve")
    public ComicReadingSourceResponseDto approve(@PathVariable Long id, @CurrentUser Long reviewerId) {
        return ComicReadingEntryMapper.toSourceResponseDto(comicReadingSourceService.approve(id, reviewerId));
    }

    @PostMapping("/reading-sources/{id}/reject")
    public ComicReadingSourceResponseDto reject(@PathVariable Long id, @CurrentUser Long reviewerId) {
        return ComicReadingEntryMapper.toSourceResponseDto(comicReadingSourceService.reject(id, reviewerId));
    }

}