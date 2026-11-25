package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import java.util.List;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSearchListingResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSearchResultDetailsResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSearchResultResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceSyncResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverLocation;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSearchListing;
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

    public static ComicReadingSearchListingResponseDto toSearchListingResponseDto(ComicReadingSearchListing listing,
            CoverUrlSigner coverUrlSigner) {
        return new ComicReadingSearchListingResponseDto(listing.providerKey(),
                listing.results()
                        .stream()
                        .map(result -> toSearchResultResponseDto(result, listing.providerKey(), coverUrlSigner))
                        .toList());
    }

    public static ComicReadingSearchResultDetailsResponseDto toSearchResultDetailsResponseDto(
            ComicReadingEntryDetails details) {
        return new ComicReadingSearchResultDetailsResponseDto(details.title(), details.url(),
                details.availableChapters(), details.latestChapterAt(), details.author(), details.status(),
                details.synopsis());
    }

    private static ComicReadingSearchResultResponseDto toSearchResultResponseDto(ComicReadingSearchResult result,
            String providerKey, CoverUrlSigner coverUrlSigner) {
        return new ComicReadingSearchResultResponseDto(result.externalId(), result.title(), result.url(),
                toCoverUrl(result.cover(), providerKey, coverUrlSigner));
    }

    // Una portada pública se entrega tal cual; una que solo sirve el provider, con la dirección firmada del
    // backend. Saber cómo se firma y qué ruta se usa es cosa de esta capa, no del dominio.
    private static String toCoverUrl(ComicReadingCoverLocation cover, String providerKey,
            CoverUrlSigner coverUrlSigner) {
        return switch (cover) {
            case null -> null;
            case ComicReadingCoverLocation.PublicUrl publicUrl -> publicUrl.url();
            case ComicReadingCoverLocation.ServedByProvider servedByProvider ->
                coverUrlSigner.signedCoverUrl(providerKey, servedByProvider.externalCoverId());
        };
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
