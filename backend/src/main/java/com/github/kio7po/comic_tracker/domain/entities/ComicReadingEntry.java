package com.github.kio7po.comic_tracker.domain.entities;

import java.time.Instant;

import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class ComicReadingEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false, length = 255)
    private String url;
    // Hidratado por el ComicReadingProvider tras la aprobación; null hasta entonces (o si no hay adaptador para la fuente).
    @Column(length = 255)
    private String title;
    // Hidratado por el ComicReadingProvider tras la aprobación; null hasta entonces (o si no hay adaptador para la fuente).
    private Integer availableChapters;
    // Código de locale estándar (BCP 47, p.ej. "es-ES", "en-US"), no un enum cerrado. Aportado por el usuario al proponer la fuente.
    @Column(nullable = false, length = 35)
    private String locale;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ComicReadingEntryStatus status = ComicReadingEntryStatus.PENDING;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "comic_id")
    private Comic comic;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private ComicReadingSource source;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contributed_by_id")
    private User contributedBy;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    // Null mientras status == PENDING; rellenado al aprobar/rechazar.
    private Instant reviewedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getAvailableChapters() {
        return availableChapters;
    }

    public void setAvailableChapters(Integer availableChapters) {
        this.availableChapters = availableChapters;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public ComicReadingEntryStatus getStatus() {
        return status;
    }

    public void setStatus(ComicReadingEntryStatus status) {
        this.status = status;
    }

    public Comic getComic() {
        return comic;
    }

    public void setComic(Comic comic) {
        this.comic = comic;
    }

    public ComicReadingSource getSource() {
        return source;
    }

    public void setSource(ComicReadingSource source) {
        this.source = source;
    }

    public User getContributedBy() {
        return contributedBy;
    }

    public void setContributedBy(User contributedBy) {
        this.contributedBy = contributedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

}
