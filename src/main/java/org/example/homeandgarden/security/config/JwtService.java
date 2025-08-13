package org.example.homeandgarden.security.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.access-token.secret}")
    private String accessTokenSecretKey;

    @Value("${jwt.refresh-token.secret}")
    private String refreshTokenSecretKey;

    @Value("${jwt.password-reset-token.secret}")
    private String passwordResetTokenSecretKey;

    @Value("${jwt.access-token.expiration}")
    private Integer accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Integer refreshTokenExpiration;

    @Value("${jwt.password-reset-token.expiration}")
    private Integer passwordResetTokenExpiration;

    public String getTokenFromRequestHeader(HttpServletRequest request) {
        final String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return null;
    }

    public String getUserEmailFromToken(String token, String secretKey) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getSignigKey(secretKey))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload().getSubject();

        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("⚠️ Error: {} | Message: {}", exception.getClass().getSimpleName(), exception.getMessage());
            throw new BadCredentialsException(String.format("%s (origin: %s)", exception.getMessage(), exception.getClass().getSimpleName()));
        } catch (Exception exception) {
            log.error("⚠️ Error: {} | Message: {}", exception.getClass().getSimpleName(), exception.getMessage());
            throw new BadCredentialsException(String.format("%s (origin: %s)", exception.getMessage(), exception.getClass().getSimpleName()));
        }
    }

    public String getUserEmailFromAccessToken(String token) {
        return getUserEmailFromToken(token, accessTokenSecretKey);
    }

    public String getUserEmailFromRefreshToken(String token) {
        return getUserEmailFromToken(token, refreshTokenSecretKey);
    }

    public String getUserEmailFromPasswordResetToken(String token) {
        return getUserEmailFromToken(token, passwordResetTokenSecretKey);
    }

    private Key getSignigKey(String jwtSecret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean isTokenValid(String token, String secretKey) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) getSignigKey(secretKey))
                    .build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("⚠️ Error: {} | Message: {}", exception.getClass().getSimpleName(), exception.getMessage());
            return false;
        } catch (Exception exception) {
            log.error("⚠️ Error: {} | Message: {}", exception.getClass().getSimpleName(), exception.getMessage());
            return false;
        }
    }

    public boolean isAccessTokenValid(String accessToken) {
        return isTokenValid(accessToken, accessTokenSecretKey);
    }

    public boolean isRefreshTokenValid(String refreshToken) {
        return isTokenValid(refreshToken, refreshTokenSecretKey);
    }

    public boolean isPasswordResetTokenValid(String passwordResetToken) {
        return isTokenValid(passwordResetToken, passwordResetTokenSecretKey);
    }

    public String generateAccessToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSignigKey(accessTokenSecretKey))
                .compact();
    }

    public String generateRefreshToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSignigKey(refreshTokenSecretKey))
                .compact();
    }

    public String generatePasswordResetToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + passwordResetTokenExpiration))
                .signWith(getSignigKey(passwordResetTokenSecretKey))
                .compact();
    }
}
