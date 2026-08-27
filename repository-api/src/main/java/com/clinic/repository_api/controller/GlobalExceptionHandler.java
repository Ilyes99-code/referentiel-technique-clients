package com.clinic.repository_api.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.clinic.repository_api.exception.AuthenticationFailedException;
import com.clinic.repository_api.exception.TooManyAttemptsException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ BadCredentialsException.class, AuthenticationFailedException.class })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleAuthenticationFailure(Exception ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied() {
        return Map.of("message", "Accès refusé");
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleTooManyAttempts(TooManyAttemptsException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(EntityNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }

    // Thrown when no controller matches the request path — was previously falling
    // through to the generic 500 handler below, e.g. GET /actuator/health without
    // the actuator dependency returned 500 instead of a clean 404.
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoResourceFound() {
        return Map.of("message", "Ressource introuvable");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = ((org.springframework.validation.FieldError) err).getField();
            fieldErrors.put(field, err.getDefaultMessage());
        });
        return Map.of("message", "Validation échouée", "errors", fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("message", ex.getMessage());
    }

    // Raised by the multipart resolver when a body exceeds
    // spring.servlet.multipart.max-*-size, i.e. before the controller is reached.
    // Without this it fell through to the generic 500 and the UI could only say
    // "erreur serveur" for what is really a client-correctable mistake.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String, String> handleUploadTooLarge() {
        return Map.of("message", "Fichier trop volumineux — 5 Mo maximum par image.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGenericException(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        return Map.of("message", "Une erreur serveur s'est produite");
    }
}
