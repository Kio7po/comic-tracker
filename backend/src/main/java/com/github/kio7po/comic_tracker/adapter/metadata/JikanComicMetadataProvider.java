package com.github.kio7po.comic_tracker.adapter.metadata;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.MediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataProvider;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

public class JikanComicMetadataProvider implements ComicMetadataProvider {

    private static final String SLUG = "myanimelist";

    @Override
    public Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsft, ComicStatus status,
            MediaType type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'search'");
    }

    @Override
    public Optional<ComicMetadataResult> fetch(String externalId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fetch'");
    }

    @Override
    public String getSourceSlug() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSourceSlug'");
    }

}
