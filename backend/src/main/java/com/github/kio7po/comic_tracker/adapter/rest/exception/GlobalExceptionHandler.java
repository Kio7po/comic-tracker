package com.github.kio7po.comic_tracker.adapter.rest.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.kio7po.comic_tracker.domain.exceptions.ComicMetadataSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.EmailAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidCredentialsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidRefreshTokenException;
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

    private static ProblemDetail problem(HttpStatus status, String detail, String type) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(type));
        return problemDetail;
    }

}
