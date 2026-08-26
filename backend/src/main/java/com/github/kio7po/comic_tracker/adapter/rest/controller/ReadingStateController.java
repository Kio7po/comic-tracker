package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ReadingStateRequestDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ReadingStateResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ReadingStateWithComicResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.ReadingStateMapper;
import com.github.kio7po.comic_tracker.adapter.rest.security.CurrentUser;
import com.github.kio7po.comic_tracker.domain.service.ComicService;
import com.github.kio7po.comic_tracker.domain.service.ReadingStateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ReadingStateController {

    private final ReadingStateService readingStateService;
    private final ComicService comicService;

    public ReadingStateController(ReadingStateService readingStateService, ComicService comicService) {
        this.readingStateService = readingStateService;
        this.comicService = comicService;
    }

    @GetMapping("/comics/{slug}/reading-state")
    public ResponseEntity<ReadingStateResponseDto> getByComic(@PathVariable String slug, @CurrentUser Long userId) {
        return comicService.findBySlug(slug)
                .flatMap(comic -> readingStateService.findByUserAndComic(userId, comic.getId()))
                .map(ReadingStateMapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/comics/{slug}/reading-state")
    public ResponseEntity<ReadingStateResponseDto> create(@PathVariable String slug,
            @Valid @RequestBody ReadingStateRequestDto request, @CurrentUser Long userId) {
        return comicService.findBySlug(slug)
                .map(comic -> readingStateService.create(userId, comic.getId(), request.status(), request.chapters(),
                        request.notes()))
                .map(readingState -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ReadingStateMapper.toResponseDto(readingState)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/comics/{slug}/reading-state")
    public ResponseEntity<ReadingStateResponseDto> update(@PathVariable String slug,
            @Valid @RequestBody ReadingStateRequestDto request, @CurrentUser Long userId) {
        return comicService.findBySlug(slug)
                .map(comic -> readingStateService.update(userId, comic.getId(), request.status(), request.chapters(),
                        request.notes()))
                .map(ReadingStateMapper::toResponseDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/comics/{slug}/reading-state")
    public ResponseEntity<Void> delete(@PathVariable String slug, @CurrentUser Long userId) {
        return comicService.findBySlug(slug).map(comic -> {
            readingStateService.delete(userId, comic.getId());
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reading-states")
    public List<ReadingStateWithComicResponseDto> findByUser(@CurrentUser Long userId) {
        return ReadingStateMapper.toWithComicResponseDtoList(readingStateService.findByUser(userId));
    }

}
