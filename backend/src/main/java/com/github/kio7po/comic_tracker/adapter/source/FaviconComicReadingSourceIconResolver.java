package com.github.kio7po.comic_tracker.adapter.source;

import java.net.URI;
import java.util.Optional;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceIconResolver;

/*
 * Fetching /favicon.ico directly from the
 * source's own domain doesn't work because some sites set a Cross-Origin-Resource-Policy header on their static
 * assets that blocks the browser from loading them as a cross-origin subresource, breaking a
 * direct &lt;img&gt; reference
*/
/**
 * Routes through Google's favicon proxy. The proxy request goes to google.com, which doesn't set
 * CORS restrictions, and Google's own crawler already handles non-standard favicon locations and
 * formats. Always matches, so it stays lowest-priority. Any source-specific resolver added later
 * should run before it.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FaviconComicReadingSourceIconResolver implements ComicReadingSourceIconResolver {

    @Override
    public boolean supports(ComicReadingSource source) {
        return true;
    }

    @Override
    public Optional<String> resolveIconUrl(ComicReadingSource source) {
        String host = URI.create(source.getUrl()).getHost();
        String iconUrl = UriComponentsBuilder.fromUriString("https://www.google.com/s2/favicons")
                .queryParam("domain", host)
                .queryParam("sz", 32)
                .toUriString();
        return Optional.of(iconUrl);
    }

}
