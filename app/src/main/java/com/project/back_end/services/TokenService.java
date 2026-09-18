package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // Build the HMAC-SHA signing key from the configured secret
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // Generate a JWT token with the given identifier as subject, expiring in 7 days
    public String generateToken(String identifier) {
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        return Jwts.builder()
                .subject(identifier)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + sevenDaysMs))
                .signWith(getSigningKey())
                .compact();
    }

    // Extract the identifier (subject) from a JWT token
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Validate the token for a specific user role; returns true if valid, false otherwise
    public boolean validateToken(String token, String user) {
        try {
            String identifier = extractEmail(token);
            if (identifier == null) return false;

            return switch (user.toLowerCase()) {
                case "admin"   -> adminRepository.findByUsername(identifier) != null;
                case "doctor"  -> doctorRepository.findByEmail(identifier) != null;
                case "patient" -> patientRepository.findByEmail(identifier) != null;
                default        -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

}
