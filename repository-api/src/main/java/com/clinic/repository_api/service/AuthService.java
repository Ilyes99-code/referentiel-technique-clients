package com.clinic.repository_api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.clinic.repository_api.dto.AuthResponse;
import com.clinic.repository_api.exception.AuthenticationFailedException;
import com.clinic.repository_api.model.User;
import com.clinic.repository_api.model.enums.RoleType;
import com.clinic.repository_api.repository.UserRepository;
import com.clinic.repository_api.security.JwtService;
import com.clinic.repository_api.security.LoginRateLimiter;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder,
            LoginRateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
    }

    public AuthResponse login(String username, String password) {
        rateLimiter.checkAllowed(username);

        try {
            AuthResponse response = authenticate(username, password);
            rateLimiter.recordSuccess(username);
            return response;
        } catch (AuthenticationFailedException e) {
            rateLimiter.recordFailure(username);
            throw e;
        }
    }

    private AuthResponse authenticate(String username, String password) {
        // Same message for "no such user" and "wrong password" so the API doesn't
        // let a caller enumerate valid usernames.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("Identifiants invalides"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationFailedException("Identifiants invalides");
        }

        if (!user.isActive()) {
            throw new AuthenticationFailedException("Ce compte a été désactivé");
        }

        if (user.getRole() != RoleType.ADMIN) {
            throw new AuthenticationFailedException("Seuls les administrateurs peuvent se connecter");
        }

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, user.getUsername(), user.getRole().name());
    }
}