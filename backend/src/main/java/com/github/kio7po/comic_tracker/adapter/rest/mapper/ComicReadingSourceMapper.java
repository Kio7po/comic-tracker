package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import java.util.List;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceSyncResponseDto;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport;
import com.github.kio7po.comic_tracker.domain.service.DiscoveryOutcome;

public final class ComicReadingSourceMapper {

    private ComicReadingSourceMapper() {
    }

    public static ComicReadingSourceResponseDto toResponseDto(ComicReadingSource source) {
        return new ComicReadingSourceResponseDto(source.getId(), source.getSlug(), source.getName(),
                source.getUrl(), source.getIconUrl(), source.getStatus(), source.getCreatedAt());
    }

    public static List<ComicReadingSourceResponseDto> toResponseDtoList(List<ComicReadingSource> sources) {
        return sources.stream().map(ComicReadingSourceMapper::toResponseDto).toList();
    }

    public static List<ComicReadingSourceModerationResponseDto> toModerationResponseDtoList(
            List<ComicReadingSource> sources) {
        return sources.stream().map(ComicReadingSourceMapper::toModerationResponseDto).toList();
    }

    public static ComicReadingSourceModerationResponseDto toModerationResponseDto(ComicReadingSource source) {
        return new ComicReadingSourceModerationResponseDto(toResponseDto(source),
                UserMapper.toContributorSummaryResponseDto(source.getContributedBy()));
    }

    public static ComicReadingSourceSyncResponseDto toSyncResponseDto(ComicReadingSourceSyncReport report) {
        return new ComicReadingSourceSyncResponseDto(toSyncedSourceDtoList(report.created()),
                toSyncedSourceDtoList(report.linked()), toSyncedSourceDtoList(report.updated()),
                report.skipped().stream().map(ComicReadingSourceMapper::toSkippedSourceDto).toList(),
                report.failedProviders().stream().map(ComicReadingSourceMapper::toFailedProviderDto).toList());
    }

    private static List<ComicReadingSourceSyncResponseDto.SyncedSource> toSyncedSourceDtoList(
            List<ComicReadingSourceSyncReport.SyncedSource> synced) {
        return synced.stream().map(item -> new ComicReadingSourceSyncResponseDto.SyncedSource(item.providerKey(),
                toResponseDto(item.source()))).toList();
    }

    private static ComicReadingSourceSyncResponseDto.SkippedSource toSkippedSourceDto(
            ComicReadingSourceSyncReport.SkippedSource skipped) {
        ComicReadingSourceDescriptor descriptor = skipped.descriptor();
        return new ComicReadingSourceSyncResponseDto.SkippedSource(skipped.providerKey(), descriptor.externalId(),
                descriptor.name(), descriptor.url(), skipped.reason());
    }

    private static ComicReadingSourceSyncResponseDto.FailedProvider toFailedProviderDto(
            DiscoveryOutcome.Failed failed) {
        String message = failed.cause().getMessage();
        return new ComicReadingSourceSyncResponseDto.FailedProvider(failed.providerKey(),
                message != null ? message : failed.cause().getClass().getSimpleName());
    }

}
