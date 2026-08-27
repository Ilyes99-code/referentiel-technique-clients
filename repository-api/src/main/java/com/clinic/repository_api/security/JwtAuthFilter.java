package com.clinic.repository_api.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)
                ? path.substring(contextPath.length())
                : path;

        return relativePath.startsWith("/api/auth/") || "/error".equals(relativePath);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                String username = jwtService.extractUsername(claims);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Re-load the user from the database on every request instead of trusting
                    // the JWT's own claims blindly — this is what makes deactivating/deleting
                    // a user actually revoke access before the token naturally expires.
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (userDetails.isEnabled()) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Rejected invalid or expired JWT: {}", e.getMessage());
            } catch (UsernameNotFoundException e) {
                log.debug("JWT subject no longer exists: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
