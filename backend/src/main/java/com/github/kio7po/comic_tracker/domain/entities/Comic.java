package com.github.kio7po.comic_tracker.domain.entities;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.MediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@Entity
public class Comic {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Column(nullable = false, unique = true)
    private String slug;
    @Column(nullable = false)
    private String title;
    private String synopsis;
    private String coverUrl;
    /*
        Usa la tabla por defecto <entity>_<attribute>
        Para la columna de unión <entity>_<PK>
        Para la columna con el dato sería <attribute>
        Importante, por defecto camelCase -> snake_case
    */
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> alternativeTitles = new HashSet<>();
    private LocalDate startDate;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private NsfwRating nsfw;
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;
    @Enumerated(EnumType.STRING)
    private ComicStatus status;
    private Integer chapters;
    @ManyToMany(fetch = FetchType.LAZY)
    /*
        Podría omitirse por completo ya que se infieren por JPA: 
        La tabla tiene de nombre <owning_table>_<inverse_table>
        Las claves son: <table_name>_<primary_key_column_name>
    */
    @JoinTable(
        name = "comic_author",
        joinColumns = @JoinColumn(name = "comic_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "author_id", referencedColumnName = "id")
    )
    private Set<Author> authors = new HashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Genre> genres = new HashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Tag> tags = new HashSet<>();

    public Comic() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public NsfwRating getNsfw() {
        return nsfw;
    }

    public void setNsfw(NsfwRating nsfw) {
        this.nsfw = nsfw;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public ComicStatus getStatus() {
        return status;
    }

    public void setStatus(ComicStatus status) {
        this.status = status;
    }

    public Integer getChapters() {
        return chapters;
    }

    public void setChapters(Integer chapters) {
        this.chapters = chapters;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    public Set<String> getAlternativeTitles() {
        return alternativeTitles;
    }

    public void setAlternativeTitles(Set<String> alternativeTitles) {
        this.alternativeTitles = alternativeTitles;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
    
}
