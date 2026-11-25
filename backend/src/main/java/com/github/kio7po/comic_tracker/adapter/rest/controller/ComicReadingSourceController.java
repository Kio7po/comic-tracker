package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSearchListingResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSearchResultDetailsResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceSyncResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.ComicReadingSourceMapper;
import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner;
import com.github.kio7po.comic_tracker.adapter.rest.security.CurrentUser;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceService;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncService;

@RestController
@RequestMapping("/api")
public class ComicReadingSourceController {

    private final ComicReadingSourceService comicReadingSourceService;
    private final ComicReadingSourceSyncService comicReadingSourceSyncService;
    private final CoverUrlSigner coverUrlSigner;

    public ComicReadingSourceController(ComicReadingSourceService comicReadingSourceService,
            ComicReadingSourceSyncService comicReadingSourceSyncService, CoverUrlSigner coverUrlSigner) {
        this.comicReadingSourceService = comicReadingSourceService;
        this.comicReadingSourceSyncService = comicReadingSourceSyncService;
        this.coverUrlSigner = coverUrlSigner;
    }

    @GetMapping("/reading-sources")
    public List<ComicReadingSourceResponseDto> findByStatusIn(@RequestParam List<ComicReadingSourceStatus> statuses,
            @RequestParam(defaultValue = "NAME") ComicReadingSourceSortField sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return ComicReadingSourceMapper
                .toResponseDtoList(comicReadingSourceService.findByStatusIn(statuses, sortBy, direction));
    }

    @GetMapping("/moderation/reading-sources")
    public List<ComicReadingSourceModerationResponseDto> findForModerationByStatusIn(
            @RequestParam List<ComicReadingSourceStatus> statuses,
            @RequestParam(defaultValue = "NAME") ComicReadingSourceSortField sortBy,
            @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return ComicReadingSourceMapper
                .toModerationResponseDtoList(comicReadingSourceService.findByStatusIn(statuses, sortBy, direction));
    }

    @GetMapping("/reading-sources/{id}/search")
    public ComicReadingSearchListingResponseDto search(@PathVariable Long id,
            @RequestParam @NotBlank @Size(max = 255) String keywords) {
        return ComicReadingSourceMapper.toSearchListingResponseDto(comicReadingSourceService.search(id, keywords),
                coverUrlSigner);
    }

    @GetMapping("/reading-sources/{id}/search-result-details")
    public ComicReadingSearchResultDetailsResponseDto searchResultDetails(@PathVariable Long id,
            @RequestParam @NotBlank @Size(max = 64) String providerKey,
            @RequestParam @NotBlank @Size(max = 255) String externalId) {
        return ComicReadingSourceMapper.toSearchResultDetailsResponseDto(
                comicReadingSourceService.fetchSearchResultDetails(id, providerKey, externalId));
    }

    @PostMapping("/moderation/reading-sources/sync")
    public ComicReadingSourceSyncResponseDto sync(@CurrentUser Long adminId) {
        return ComicReadingSourceMapper.toSyncResponseDto(comicReadingSourceSyncService.sync(adminId));
    }

    @PostMapping("/reading-sources/{id}/approve")
    public ComicReadingSourceResponseDto approve(@PathVariable Long id, @CurrentUser Long reviewerId) {
        return ComicReadingSourceMapper.toResponseDto(comicReadingSourceService.approve(id, reviewerId));
    }

    @PostMapping("/reading-sources/{id}/reject")
    public ComicReadingSourceResponseDto reject(@PathVariable Long id, @CurrentUser Long reviewerId) {
        return ComicReadingSourceMapper.toResponseDto(comicReadingSourceService.reject(id, reviewerId));
    }

}