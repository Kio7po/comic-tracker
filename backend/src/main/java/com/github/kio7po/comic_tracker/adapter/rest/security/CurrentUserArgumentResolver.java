package com.github.kio7po.comic_tracker.adapter.rest.security;

import java.security.Principal;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.github.kio7po.comic_tracker.domain.exceptions.InvalidCredentialsException;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Principal principal = webRequest.getUserPrincipal();
        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new InvalidCredentialsException();
        }

        String subject = jwt.getSubject();
        if (subject == null) {
            // "sub" is optional per the JWT spec; token is external input, so don't assume it.
            throw new InvalidCredentialsException();
        }

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new InvalidCredentialsException();
        }
    }

}
