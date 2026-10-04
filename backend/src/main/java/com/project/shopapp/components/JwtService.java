package com.project.shopapp.components;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expiration;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.access-token-expiration}") long expiration) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        String role =
                user.getAuthorities().stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse("ROLE_USER");
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public Instant extractExpiration(String token) {
        return claims(token).getExpiration().toInstant();
    }

    public long getExpiration() {
        return expiration;
    }

    public boolean isValid(String token, UserDetails user) {
        try {
            Claims claims = claims(token);
            return claims.getSubject().equals(user.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
