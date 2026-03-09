package com.example.userservice.auth;

import com.example.userservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "sub";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String CLAIM_ROLES = "roles";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(Long userId, List<String> roles) {
        Date now = new Date();
        Date expiry = Date.from(now.toInstant().plus(properties.accessTokenValidity()));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLES, roles != null ? roles : List.<String>of())
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String issueRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = Date.from(now.toInstant().plus(properties.refreshTokenValidity()));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public JwtClaims validateAccessToken(String token) {
        return validateToken(token, TYPE_ACCESS);
    }

    public JwtClaims validateRefreshToken(String token) {
        return validateToken(token, TYPE_REFRESH);
    }

    /** Extract roles claim in a type-safe way (avoids unchecked cast from raw List). */
    private static List<String> getRolesFromClaims(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String s) {
                result.add(s);
            }
        }
        return result;
    }

    private JwtClaims validateToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .require(CLAIM_TOKEN_TYPE, expectedType)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new JwtException("Missing subject");
            }
            Long userId = Long.parseLong(sub);
            List<String> roles = getRolesFromClaims(claims);
            return new JwtClaims(userId, roles, claims.getExpiration().toInstant());
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token expired", e);
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Invalid token", e);
        }
    }

    public record JwtClaims(Long userId, List<String> roles, java.time.Instant expiresAt) {}
}
