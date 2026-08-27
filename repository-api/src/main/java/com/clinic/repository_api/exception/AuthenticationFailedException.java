package com.clinic.repository_api.exception;

/**
 * Raised for a rejected login attempt (unknown user, bad password, wrong role).
 * Kept distinct from IllegalArgumentException so a 401 response is tied to an
 * explicit auth failure instead of any generic bad-argument condition.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
