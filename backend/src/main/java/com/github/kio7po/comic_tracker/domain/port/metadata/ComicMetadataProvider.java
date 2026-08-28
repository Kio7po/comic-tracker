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
     * @param sortBy    null leaves the ordering entirely up to the provider (e.g. no explicit sort
     *                  requested, so the provider's own default/relevance order is used).
     * @param direction null leaves the direction up to the provider's own default for the given
     *                  sortBy.
     */
    Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsfw, ComicStatus status,
            ComicMediaType type, ComicSearchSortField sortBy, SortDirection direction);
    Optional<ComicMetadataResult> fetch(String externalId);
    String getSourceSlug();
}
