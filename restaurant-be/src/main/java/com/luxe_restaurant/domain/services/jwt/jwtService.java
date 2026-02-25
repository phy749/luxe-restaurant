package com.luxe_restaurant.domain.services.jwt;

import com.luxe_restaurant.domain.entities.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@Slf4j
public class jwtService {

    // Keep the existing secret key but make it configurable
    @Value("${security.jwt.secret:79a6404a6bb4a8bf6f3912a5b652fcc2ea2c153a1dfc5f5772acb525916599be979414c7a757b890205093dde9a68ea0d127e514823e54c4260835b70c3dd8a6}")
    private String secretKey;
    
    @Value("${security.jwt.expiration:1800000}") // 30 minutes default
    private long accessTokenExpiration;
    
    @Value("${security.jwt.refresh-expiration:2592000000}") // 30 days default
    private long refreshTokenExpiration;

    public String generateAccessToken(User user) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            Date issueTime = new Date();
            Date expiredTime = new Date(issueTime.getTime() + accessTokenExpiration);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUserName())
                    .claim("userId", user.getId())
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole().name())
                    .issuer("luxe-restaurant")
                    .audience("luxe-restaurant-client")
                    .issueTime(issueTime)
                    .expirationTime(expiredTime)
                    .build();

            Payload payload = new Payload(claimsSet.toJSONObject());
            JWSObject jwsObject = new JWSObject(header, payload);
            
            jwsObject.sign(new MACSigner(secretKey));
            
            log.debug("Generated access token for user: {}", user.getEmail());
            return jwsObject.serialize();
            
        } catch (JOSEException e) {
            log.error("Error generating access token for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public String generateRefreshToken(User user) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            Date issueTime = new Date();
            Date expiredTime = new Date(issueTime.getTime() + refreshTokenExpiration);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUserName())
                    .claim("userId", user.getId())
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole().name())
                    .claim("tokenType", "refresh")
                    .issuer("luxe-restaurant")
                    .audience("luxe-restaurant-client")
                    .issueTime(issueTime)
                    .expirationTime(expiredTime)
                    .build();

            Payload payload = new Payload(claimsSet.toJSONObject());
            JWSObject jwsObject = new JWSObject(header, payload);
            
            jwsObject.sign(new MACSigner(secretKey));
            
            log.debug("Generated refresh token for user: {}", user.getEmail());
            return jwsObject.serialize();
            
        } catch (JOSEException e) {
            log.error("Error generating refresh token for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }
    
    public boolean isTokenExpired(String token) {
        try {
            // Implementation for token validation would go here
            // This is a simplified version
            return false;
        } catch (Exception e) {
            log.error("Error validating token", e);
            return true;
        }
    }
}
