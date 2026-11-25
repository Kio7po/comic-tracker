package com.github.kio7po.comic_tracker.domain.port.source;

/**
 * A cover's bytes with their media type. A class rather than a record because a record's equality over an array
 * compares identity, not content.
 */
public final class ComicReadingCoverImage {

    private final byte[] content;
    private final String contentType;

    public ComicReadingCoverImage(byte[] content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

}
