package com.github.kio7po.comic_tracker.domain.port.metadata;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.enums.ComicSearchSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;

public interface ComicMetadataProvider {
    /**
     * @param sortBy    null leaves the sort order entirely up to the provider's own default, which
     *                  is provider-specific. {@code RELEVANCE} is a distinct, explicit request to
     *                  sort by relevance specifically.
     * @param direction has no effect when sortBy is {@code RELEVANCE}.
     */
    Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsfw, ComicStatus status,
            ComicMediaType type, ComicSearchSortField sortBy, SortDirection direction);
    Optional<ComicMetadataResult> fetch(String externalId);
    String getSourceSlug();
}
