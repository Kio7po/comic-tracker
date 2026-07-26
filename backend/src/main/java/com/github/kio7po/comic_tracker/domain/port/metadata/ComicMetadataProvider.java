package com.github.kio7po.comic_tracker.domain.port.metadata;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.MediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;

public interface ComicMetadataProvider {
    Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsft, ComicStatus status, MediaType type);
    Optional<ComicMetadataResult> fetch(String externalId);
    String getSourceSlug();
}
