package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.domain.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service: JwtService
 * Handles JWT token generation and validation
 * Part of Clean Architecture - Infrastructure Layer
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateAccessToken(User user) {
        return generateAccessToken(user, null);
    }

    /**
     * @param providerScopeId optional {@code pid} claim for RLS / provider portal scoping
     */
    public String generateAccessToken(User user, UUID providerScopeId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("tv", user.getTokenVersion());
        if (providerScopeId != null) {
            claims.put("pid", providerScopeId.toString());
        }
        return generateToken(claims, user.getId().toString(), jwtExpiration);
    }
    
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tv", user.getTokenVersion());
        return generateToken(claims, user.getId().toString(), refreshExpiration);
    }
    
    private String generateToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * JWT ID claim (jti), used for revocation until expiry.
     */
    public String extractJti(String token) {
        try {
            return extractClaim(token, Claims::getId);
        } catch (Exception e) {
            return null;
        }
    }

    public Instant extractExpirationInstant(String token) {
        return extractExpiration(token).toInstant();
    }
    
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Token version for password-rotation invalidation; missing claim defaults to 0 (legacy tokens).
     */
    public int extractTokenVersion(String token) {
        return extractClaim(token, claims -> {
            Object raw = claims.get("tv");
            if (raw instanceof Integer i) {
                return i;
            }
            if (raw instanceof Number n) {
                return n.intValue();
            }
            return 0;
        });
    }

    /**
     * Provider scope for RLS ({@code pid} claim); absent for customers / staff without a home provider.
     */
    public UUID extractProviderScopeId(String token) {
        try {
            return extractClaim(token, claims -> {
                Object raw = claims.get("pid");
                if (raw instanceof String s && !s.isBlank()) {
                    return UUID.fromString(s.trim());
                }
                return null;
            });
        } catch (Exception e) {
            return null;
        }
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
    
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public long getExpirationTime() {
        return jwtExpiration / 1000; // Return in seconds
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpiration / 1000;
    }
}
