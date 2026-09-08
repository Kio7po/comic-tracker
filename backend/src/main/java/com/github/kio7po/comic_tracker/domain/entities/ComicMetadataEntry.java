package com.github.kio7po.comic_tracker.domain.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ComicMetadataEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false, length = 255)
    private String externalId;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id") // optional, defaults to this
    private Comic comic;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private ComicMetadataSource source;
    @Column(nullable = false)
    private Instant lastFetchedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Comic getComic() {
        return comic;
    }

    public void setComic(Comic comic) {
        this.comic = comic;
    }

    public ComicMetadataSource getSource() {
        return source;
    }

    public void setSource(ComicMetadataSource source) {
        this.source = source;
    }

    public Instant getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(Instant lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }

}
