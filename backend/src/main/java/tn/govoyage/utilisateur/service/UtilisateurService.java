package tn.govoyage.utilisateur.service;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import tn.govoyage.exception.EmailDejaUtiliseException;
import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.dto.InscriptionRequest;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.entity.Role;
import tn.govoyage.utilisateur.entity.Utilisateur;

/** Logique metier des comptes utilisateurs (inscription, conversion en reponse). */
@Service
public class UtilisateurService {

    /** DAO des utilisateurs. */
    private final UtilisateurDAO utilisateurDAO;

    /** Hacheur de mot de passe (BCrypt). */
    private final PasswordEncoder passwordEncoder;

    /** Prend le DAO et le hacheur de mot de passe. */
    public UtilisateurService(UtilisateurDAO utilisateurDAO, PasswordEncoder passwordEncoder) {
        this.utilisateurDAO = utilisateurDAO;
        this.passwordEncoder = passwordEncoder;
    }

    /** Inscrit un nouvel utilisateur et renvoie sa reponse (toujours role USER). */
    public UtilisateurResponse inscrire(InscriptionRequest demande) {
        return creer(demande, Role.USER);
    }

    /** Cree un compte avec le role determine par le service appelant. */
    UtilisateurResponse creer(InscriptionRequest demande, Role role) {
        String email = demande.email().toLowerCase(Locale.ROOT);
        if (utilisateurDAO.emailExiste(email)) {
            throw new EmailDejaUtiliseException();
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(demande.nom());
        utilisateur.setPrenom(demande.prenom());
        utilisateur.setEmail(email);
        utilisateur.setPassword(passwordEncoder.encode(demande.motDePasse()));
        utilisateur.setTel(demande.tel());
        utilisateur.setRole(role);
        return enregistrer(utilisateur);
    }

    /** Traduit un conflit d'e-mail meme lorsque deux requetes enregistrent en meme temps. */
    UtilisateurResponse enregistrer(Utilisateur utilisateur) {
        try {
            return versReponse(utilisateurDAO.saveAndFlush(utilisateur));
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception.getMostSpecificCause();
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                throw new EmailDejaUtiliseException();
            }
            throw exception;
        }
    }

    /** Convertit une entite utilisateur en reponse, sans le mot de passe. */
    public static UtilisateurResponse versReponse(Utilisateur utilisateur) {
        return new UtilisateurResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getTel(),
                utilisateur.getRole().name());
    }
}
