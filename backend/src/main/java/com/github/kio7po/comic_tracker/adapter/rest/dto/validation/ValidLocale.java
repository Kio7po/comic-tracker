package com.github.kio7po.comic_tracker.adapter.rest.dto.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A valid ISO 639-1 language code, optionally followed by a valid ISO 3166-1 alpha-2 region
 * (e.g. {@code es} or {@code es-ES}).
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidLocaleValidator.class)
public @interface ValidLocale {

    String message() default "must be a valid ISO language code, optionally followed by a valid ISO region (e.g. 'es' or 'es-ES')";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
