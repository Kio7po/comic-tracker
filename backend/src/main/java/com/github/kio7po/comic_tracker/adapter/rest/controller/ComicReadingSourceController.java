package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.ComicReadingEntryMapper;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceService;

@RestController
@RequestMapping("/api/reading-sources")
public class ComicReadingSourceController {

    private final ComicReadingSourceService comicReadingSourceService;

    public ComicReadingSourceController(ComicReadingSourceService comicReadingSourceService) {
        this.comicReadingSourceService = comicReadingSourceService;
    }

    @GetMapping
    public List<ComicReadingSourceResponseDto> findSelectable() {
        return ComicReadingEntryMapper.toSourceResponseDtoList(comicReadingSourceService.findSelectable());
    }

}