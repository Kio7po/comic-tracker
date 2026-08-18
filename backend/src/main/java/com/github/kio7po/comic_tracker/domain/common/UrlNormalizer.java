package com.github.kio7po.comic_tracker.domain.common;

import java.net.URI;
import java.util.Locale;

public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    /**
     * Deliberately conservative: lowercases scheme/host and drops one trailing slash, but leaves
     * path/query/fragment untouched, since those can be meaningful (e.g. a specific chapter).
     */
    public static String normalize(String url) {
        URI uri = URI.create(url);

        StringBuilder normalized = new StringBuilder(origin(uri));
        if (uri.getRawPath() != null) {
            normalized.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null) {
            normalized.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            normalized.append('#').append(uri.getRawFragment());
        }

        return normalized.charAt(normalized.length() - 1) == '/' ? normalized.substring(0, normalized.length() - 1)
                : normalized.toString();
    }

    /**
     * Collapses to just scheme+host, discarding path/query/fragment, to identifying the site
     * itself, not a specific page on it.
     */
    public static String normalizeOrigin(String url) {
        return origin(URI.create(url));
    }

    private static String origin(URI uri) {
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getAuthority().toLowerCase(Locale.ROOT);
    }

}
