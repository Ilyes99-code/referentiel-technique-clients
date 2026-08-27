package com.clinic.repository_api.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public String generateAccessToken(UserDetails userDetails) {
        String authority = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot issue a token for a user with no granted authority: " + userDetails.getUsername()));

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(ROLE_CLAIM, authority)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Throws JwtException/IllegalArgumentException on a malformed, expired, or
     * badly-signed token. Callers must treat any exception as "unauthenticated".
     */
    public Claims parseClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        return claims.get(ROLE_CLAIM, String.class);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
