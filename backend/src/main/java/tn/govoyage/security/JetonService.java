package tn.govoyage.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tn.govoyage.utilisateur.entity.Utilisateur;

@Service
public class JetonService {

    @Value("${go-voyage.jeton.secret}")
    private String secret;

    @Value("${go-voyage.jeton.duree-secondes}")
    private long dureeSecondes;

    public String genererJeton(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim("id", utilisateur.getId())
                .claim("role", utilisateur.getRole().name())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plusSeconds(dureeSecondes)))
                .signWith(cle(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims lireJeton(String jeton) {
        try {
            return Jwts.parser().verifyWith(cle()).build().parseSignedClaims(jeton).getPayload();
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
