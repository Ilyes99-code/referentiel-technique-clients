package com.clinic.repository_api.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Without this, Spring Security's default entry point (Http403ForbiddenEntryPoint)
 * answers every unauthenticated request — missing, malformed, or expired JWT alike —
 * with an empty-body 403. The frontend has no way to tell "you need to log in" apart
 * from "you're not allowed", and never triggers its own logout flow. This makes that
 * case a real 401 with a JSON body matching GlobalExceptionHandler's shape.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", "Authentification requise"));
    }
}
