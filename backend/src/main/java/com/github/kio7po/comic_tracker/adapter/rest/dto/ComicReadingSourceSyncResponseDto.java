package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.util.List;

import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SkipReason;

public record ComicReadingSourceSyncResponseDto(
        List<SyncedSource> created,
        List<SyncedSource> linked,
        List<SyncedSource> updated,
        List<SkippedSource> skipped,
        List<FailedProvider> failedProviders) {

    public record SyncedSource(String providerKey, ComicReadingSourceResponseDto source) {
    }

    public record SkippedSource(String providerKey, String externalId, String name, String url, SkipReason reason) {
    }

    public record FailedProvider(String providerKey, String message) {
    }
}
