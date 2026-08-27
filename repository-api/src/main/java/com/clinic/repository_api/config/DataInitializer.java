package com.clinic.repository_api.config;

import com.clinic.repository_api.model.User;
import com.clinic.repository_api.model.enums.RoleType;
import com.clinic.repository_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username}") String bootstrapUsername,
            @Value("${app.bootstrap-admin.password}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(bootstrapUsername)) {
            User admin = new User();
            admin.setUsername(bootstrapUsername);
            admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
            admin.setRole(RoleType.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
            log.warn("Default admin account '{}' created with the configured bootstrap password — change it after first login.",
                    bootstrapUsername);
        }
    }
}
