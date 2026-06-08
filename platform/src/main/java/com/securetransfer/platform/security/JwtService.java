package com.securetransfer.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final StringRedisTemplate redisTemplate;

    public JwtService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Générer un token d'accès (15 minutes)
    public String generateAccessToken(String email, UUID userId) {
        return generateToken(email, userId, expiration);
    }

    // Générer un refresh token (7 jours)
    public String generateRefreshToken(String email, UUID userId) {
        return generateToken(email, userId, refreshExpiration);
    }

    // Méthode privée pour générer un token
    private String generateToken(String email, UUID userId, long expirationMs) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .id(UUID.randomUUID().toString()) // JTI unique pour blacklist
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    // Extraire l'email depuis le token
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extraire le userId depuis le token
    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    // Extraire le JTI (ID unique du token)
    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    // Vérifier si le token est valide (signature + expiration + non blacklisté)
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean isExpired = claims.getExpiration().before(new Date());
            boolean isBlacklisted = isTokenBlacklisted(extractJti(token));
            return !isExpired && !isBlacklisted;
        } catch (SignatureException e) {
            return false;
        }
    }

    // Extraire toutes les informations du token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Récupérer la clé de signature
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Blacklister un token lors de la déconnexion
    public void blacklistToken(String token) {
        String jti = extractJti(token);
        Date expiration = extractAllClaims(token).getExpiration();
        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:" + jti,
                    "true",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    // Vérifier si un token est blacklisté
    private boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
    }
}