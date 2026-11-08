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

/** Cree et lit les jetons JWT (algorithme HS256). */
@Service
public class JetonService {

    /** Cle secrete de signature, prise dans la configuration. */
    @Value("${go-voyage.jeton.secret}")
    private String secret;

    /** Duree de validite du jeton en secondes (86400 = 24 heures). */
    @Value("${go-voyage.jeton.duree-secondes}")
    private long dureeSecondes;

    /** Genere un jeton pour un compte donne (sujet = e-mail, role dans les claims). */
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

    /** Lit un jeton correctement signe et non expire. */
    public Claims lireJeton(String jeton) {
        try {
            return Jwts.parser().verifyWith(cle()).build().parseSignedClaims(jeton).getPayload();
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    /** Prend la chaine secrete et renvoie la cle de signature HS256. */
    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
