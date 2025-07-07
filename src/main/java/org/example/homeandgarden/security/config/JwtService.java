package org.example.homeandgarden.security.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.homeandgarden.user.repository.UserRepository;
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

    private final UserRepository userRepository;

    @Value("${jwt.access-token.secret}")
    private String accessTokenSecretKey;

    @Value("${jwt.refresh-token.secret}")
    private String refreshTokenSecretKey;

    @Value("${jwt.access-token.expiration}")
    private Integer accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Integer refreshTokenExpiration;

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


    private Key getSignigKey(String jwtSecret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateRefreshToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSignigKey(refreshTokenSecretKey))
                .compact();
    }

    public String generateAccessToken(String email) {

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSignigKey(accessTokenSecretKey))
                .compact();
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

        if (isTokenValid(accessToken, accessTokenSecretKey)) {

            String email = getUserEmailFromAccessToken(accessToken);
            if (email == null) {
                throw new BadCredentialsException("Token does not contain a user identifier. Please log in again.");
            }

            if (!userRepository.existsByEmail(email)) {
                throw new BadCredentialsException("User email does not match the one in the database. Please log in again.");
            }
            return true;
        }
        return false;
    }

    public boolean isRefreshTokenValid(String refreshToken) {

        if (isTokenValid(refreshToken, refreshTokenSecretKey)) {

            String email = getUserEmailFromRefreshToken(refreshToken);
            if (email == null) {
                throw new BadCredentialsException("Token does not contain a user identifier. Please log in again.");
            }

            if (!userRepository.existsByEmailAndRefreshToken(email, refreshToken)) {
                throw new BadCredentialsException("Email or refresh token do not match those in the database. Please log in again.");
            }
            return true;
        }
        return false;
    }
}
