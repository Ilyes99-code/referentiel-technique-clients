package com.clinic.repository_api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.clinic.repository_api.security.JwtAuthFilter;
import com.clinic.repository_api.security.RestAccessDeniedHandler;
import com.clinic.repository_api.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    // Comma-separated origin list, e.g. "http://localhost:5173,https://app.example.com".
    // Empty by default (fail closed) — cross-origin browser calls are blocked unless
    // explicitly opted into per environment.
    //
    // Injected via the constructor rather than a @Value field: this bean can be
    // eagerly instantiated by Spring Security's AuthenticationManager bootstrap
    // machinery (see the "Eagerly initializing" log line at startup), and constructor
    // injection is the only form guaranteed to resolve before any other method on
    // this bean — including its own @Bean factory methods — can run.
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            UserDetailsService userDetailsService,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    // A single chain covering every request. /api/auth/**, /error, /actuator/health
    // and /h2-console/** are opened up via permitAll() below instead of
    // WebSecurityCustomizer#ignoring(), which used to bypass the filter chain
    // entirely (including security headers) for those paths — Spring Security's
    // own docs advise against that pattern.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                // CORS preflight (OPTIONS) requests never carry credentials/Authorization —
                // without this, anyRequest().authenticated() rejects them with 403 before
                // the CorsFilter gets a chance to answer, and the browser never even
                // attempts the real request.
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .requestMatchers("/api/auth/**", "/error", "/actuator/health", "/h2-console/**").permitAll()
                // OpenAPI spec + Swagger UI. Reachable without a token because the UI
                // has to load before you can authenticate through it — you cannot put
                // the login form behind the login.
                //
                // These paths only exist when springdoc is enabled, which is the dev
                // default and opt-in elsewhere (SPRINGDOC_ENABLED, see
                // application-prod.properties). Disabled, they simply 404. That switch
                // is the control, not this matcher: an enabled Swagger UI publishes the
                // full endpoint inventory and every DTO shape to anyone who can reach
                // the port.
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}