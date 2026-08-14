package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceIconResolver;

@Component
public class ComicReadingSourceIconResolverRegistry {

    private final List<ComicReadingSourceIconResolver> resolvers;

    public ComicReadingSourceIconResolverRegistry(List<ComicReadingSourceIconResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public Optional<String> resolveIconUrl(ComicReadingSource source) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(source))
                .findFirst()
                .flatMap(resolver -> resolver.resolveIconUrl(source));
    }

}
