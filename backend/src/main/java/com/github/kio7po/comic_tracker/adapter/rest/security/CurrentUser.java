package com.github.kio7po.comic_tracker.adapter.rest.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves a {@code Long} controller method parameter to the id of the user behind the
 * request's JWT. Throws {@code InvalidCredentialsException} if there's no valid JWT principal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {
}
