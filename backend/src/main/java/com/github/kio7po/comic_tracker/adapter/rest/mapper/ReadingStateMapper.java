package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import java.util.List;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ReadingStateResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ReadingStateWithComicResponseDto;
import com.github.kio7po.comic_tracker.domain.entities.ReadingState;

public final class ReadingStateMapper {

    private ReadingStateMapper() {
    }

    public static ReadingStateResponseDto toResponseDto(ReadingState readingState) {
        return new ReadingStateResponseDto(readingState.getId(), readingState.getStatus(),
                readingState.getChapters(), readingState.getCreatedAt(), readingState.getUpdatedAt());
    }

    public static List<ReadingStateWithComicResponseDto> toWithComicResponseDtoList(
            List<ReadingState> readingStates) {
        return readingStates.stream().map(ReadingStateMapper::toWithComicResponseDto).toList();
    }

    public static ReadingStateWithComicResponseDto toWithComicResponseDto(ReadingState readingState) {
        return new ReadingStateWithComicResponseDto(toResponseDto(readingState),
                ComicReadingEntryMapper.toComicSummaryResponseDto(readingState.getComic()));
    }

}
