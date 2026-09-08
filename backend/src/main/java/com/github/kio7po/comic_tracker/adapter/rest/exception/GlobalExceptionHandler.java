package com.github.kio7po.comic_tracker.adapter.rest.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.kio7po.comic_tracker.domain.exceptions.ComicMetadataSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingSourceException;
import com.github.kio7po.comic_tracker.domain.exceptions.EmailAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidCredentialsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidPreferredReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidRefreshTokenException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.UnsupportedMetadataSourceException;
import com.github.kio7po.comic_tracker.domain.exceptions.UsernameAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.WeakPasswordException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ProblemDetail handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.USERNAME_ALREADY_EXISTS);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.EMAIL_ALREADY_EXISTS);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail handleWeakPassword(WeakPasswordException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), ProblemType.WEAK_PASSWORD);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), ProblemType.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), ProblemType.INVALID_REFRESH_TOKEN);
    }

    @ExceptionHandler(UnsupportedMetadataSourceException.class)
    public ProblemDetail handleUnsupportedMetadataSource(UnsupportedMetadataSourceException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), ProblemType.UNSUPPORTED_METADATA_SOURCE);
    }

    @ExceptionHandler(ComicMetadataSourceNotFoundException.class)
    public ProblemDetail handleComicMetadataSourceNotFound(ComicMetadataSourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ProblemType.METADATA_SOURCE_NOT_FOUND);
    }

    @ExceptionHandler(ComicNotFoundException.class)
    public ProblemDetail handleComicNotFound(ComicNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ProblemType.COMIC_NOT_FOUND);
    }

    @ExceptionHandler(ComicReadingSourceNotFoundException.class)
    public ProblemDetail handleComicReadingSourceNotFound(ComicReadingSourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ProblemType.READING_SOURCE_NOT_FOUND);
    }

    @ExceptionHandler(ComicReadingEntryNotFoundException.class)
    public ProblemDetail handleComicReadingEntryNotFound(ComicReadingEntryNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ProblemType.READING_ENTRY_NOT_FOUND);
    }

    @ExceptionHandler(DuplicateComicReadingSourceException.class)
    public ProblemDetail handleDuplicateComicReadingSource(DuplicateComicReadingSourceException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.DUPLICATE_READING_SOURCE);
    }

    @ExceptionHandler(DuplicateComicReadingEntryException.class)
    public ProblemDetail handleDuplicateComicReadingEntry(DuplicateComicReadingEntryException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.DUPLICATE_READING_ENTRY);
    }

    @ExceptionHandler(ComicReadingEntryAlreadyReviewedException.class)
    public ProblemDetail handleComicReadingEntryAlreadyReviewed(ComicReadingEntryAlreadyReviewedException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.READING_ENTRY_ALREADY_REVIEWED);
    }

    @ExceptionHandler(ComicReadingSourceNotApprovedException.class)
    public ProblemDetail handleComicReadingSourceNotApproved(ComicReadingSourceNotApprovedException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.READING_SOURCE_NOT_APPROVED);
    }

    @ExceptionHandler(ComicReadingSourceAlreadyReviewedException.class)
    public ProblemDetail handleComicReadingSourceAlreadyReviewed(ComicReadingSourceAlreadyReviewedException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.READING_SOURCE_ALREADY_REVIEWED);
    }

    @ExceptionHandler(ReadingStateNotFoundException.class)
    public ProblemDetail handleReadingStateNotFound(ReadingStateNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), ProblemType.READING_STATE_NOT_FOUND);
    }

    @ExceptionHandler(ReadingStateAlreadyExistsException.class)
    public ProblemDetail handleReadingStateAlreadyExists(ReadingStateAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), ProblemType.READING_STATE_ALREADY_EXISTS);
    }

    @ExceptionHandler(InvalidPreferredReadingEntryException.class)
    public ProblemDetail handleInvalidPreferredReadingEntry(InvalidPreferredReadingEntryException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), ProblemType.INVALID_PREFERRED_READING_ENTRY);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String type) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(type));
        return problemDetail;
    }

}
