package com.example.paymentservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates access tokens issued by user-service (shared secret).
 */
@Component
public class JwtValidator {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TYPE_ACCESS = "access";

    private final JwtValidatorProperties properties;
    private final SecretKey signingKey;

    public JwtValidator(JwtValidatorProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate access token and return user id (sub claim). Throws on invalid/expired.
     */
    public Long validateAccessTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .require(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new JwtException("Missing subject");
            }
            return Long.parseLong(sub);
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token expired", e);
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Invalid token", e);
        }
    }
}
