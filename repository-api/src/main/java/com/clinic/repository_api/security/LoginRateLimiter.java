package com.clinic.repository_api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.clinic.repository_api.exception.TooManyAttemptsException;

/**
 * In-memory brute-force guard for /api/auth/login, keyed by username.
 *
 * This is single-instance only (state isn't shared across nodes) — fine for the
 * current single-instance deployment, but if this app is ever run behind a load
 * balancer with multiple replicas, swap this for a shared store (e.g. Redis).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    public void checkAllowed(String username) {
        Attempts attempts = attemptsByUsername.get(normalize(username));
        if (attempts != null && attempts.isLocked()) {
            throw new TooManyAttemptsException(
                    "Trop de tentatives échouées. Réessayez dans quelques minutes.");
        }
    }

    public void recordFailure(String username) {
        attemptsByUsername.compute(normalize(username), (key, existing) -> {
            Attempts attempts = (existing == null || existing.isExpired()) ? new Attempts() : existing;
            attempts.increment();
            return attempts;
        });
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class Attempts {
        private int count = 0;
        private Instant windowStart = Instant.now();

        synchronized void increment() {
            if (isExpired()) {
                count = 0;
                windowStart = Instant.now();
            }
            count++;
        }

        synchronized boolean isExpired() {
            return Instant.now().isAfter(windowStart.plus(LOCKOUT_WINDOW));
        }

        synchronized boolean isLocked() {
            return !isExpired() && count >= MAX_ATTEMPTS;
        }
    }
}
