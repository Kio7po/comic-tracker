package com.github.kio7po.comic_tracker.adapter.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.github.kio7po.comic_tracker.domain.exceptions.EmailAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.UsernameAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.WeakPasswordException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ UsernameAlreadyExistsException.class, EmailAlreadyExistsException.class })
    public ProblemDetail handleAlreadyExists(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail handleWeakPassword(WeakPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

}
