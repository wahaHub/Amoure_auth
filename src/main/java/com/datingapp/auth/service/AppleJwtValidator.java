package com.datingapp.auth.service;

import com.datingapp.auth.config.AppleProperties;
import com.datingapp.auth.exception.AuthenticationException;
import com.datingapp.auth.model.ApplePublicKey;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleJwtValidator {
    private final AppleProperties properties;
    private final ApplePublicKeyCache publicKeyCache;
    
    private static final long MAX_TOKEN_AGE = 24 * 60 * 60 * 1000; // 24 hours

    public Claims validateToken(String identityToken) {
        try {
            // 1. Parse the JWT without verification to get the header
            String[] tokenParts = identityToken.split("\\.");
            if (tokenParts.length != 3) {
                throw new AuthenticationException("Invalid token format");
            }

            // 2. Parse header to get key ID
            String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
            String keyId = extractKeyId(headerJson);
            
            // 3. Get the corresponding public key
            ApplePublicKey publicKey = publicKeyCache.getPublicKey(keyId);
            if (publicKey == null) {
                throw new AuthenticationException("No matching public key found");
            }

            // 4. Convert Apple public key to RSA public key
            PublicKey rsaPublicKey = generatePublicKey(publicKey);

            // 5. Parse and validate the token
            JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(rsaPublicKey)
                .requireIssuer("https://appleid.apple.com")
                .requireAudience(properties.getClientId())
                .build();

            Jws<Claims> jws = parser.parseClaimsJws(identityToken);
            Claims claims = jws.getBody();

            // 6. Additional validations
            validateTokenAge(claims);
            validateMandatoryClaims(claims);

            return claims;

        } catch (ExpiredJwtException e) {
            log.error("Apple token expired", e);
            throw new AuthenticationException("Apple identity token has expired");
        } catch (SignatureException e) {
            log.error("Invalid token signature", e);
            throw new AuthenticationException("Invalid Apple identity token signature");
        } catch (Exception e) {
            log.error("Failed to validate Apple identity token", e);
            throw new AuthenticationException("Invalid Apple identity token: " + e.getMessage());
        }
    }

    private String extractKeyId(String headerJson) {
        try {
            int kidStart = headerJson.indexOf("\"kid\":\"") + 7;
            int kidEnd = headerJson.indexOf("\"", kidStart);
            return headerJson.substring(kidStart, kidEnd);
        } catch (Exception e) {
            throw new AuthenticationException("Failed to extract key ID from token");
        }
    }

    private PublicKey generatePublicKey(ApplePublicKey publicKey) throws Exception {
        // Convert base64 encoded modulus and exponent to BigInteger
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(publicKey.getN()));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(publicKey.getE()));

        // Create RSA Public Key from components
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    private void validateTokenAge(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null) {
            throw new AuthenticationException("Token missing issuedAt claim");
        }

        long age = System.currentTimeMillis() - issuedAt.getTime();
        if (age > MAX_TOKEN_AGE) {
            throw new AuthenticationException("Token too old");
        }
    }

    private void validateMandatoryClaims(Claims claims) {
        // Validate required claims
        if (claims.getSubject() == null) {
            throw new AuthenticationException("Token missing subject (user ID)");
        }
        if (claims.get("email") == null) {
            throw new AuthenticationException("Token missing email claim");
        }
        if (claims.get("email_verified") == null) {
            throw new AuthenticationException("Token missing email verification status");
        }
    }
} 