package tn.govoyage.utilisateur.service;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.govoyage.exception.EmailDejaUtiliseException;
import tn.govoyage.exception.RessourceIntrouvableException;
import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.dto.InscriptionRequest;
import tn.govoyage.utilisateur.dto.ModificationProfilRequest;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.entity.Role;
import tn.govoyage.utilisateur.entity.Utilisateur;

@Service
public class UtilisateurService {

    private final UtilisateurDAO utilisateurDAO;

    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurDAO utilisateurDAO, PasswordEncoder passwordEncoder) {
        this.utilisateurDAO = utilisateurDAO;
        this.passwordEncoder = passwordEncoder;
    }

    public UtilisateurResponse inscrire(InscriptionRequest demande) {
        return creer(demande, Role.USER);
    }

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

    @Transactional
    public UtilisateurResponse modifierProfil(String email, ModificationProfilRequest demande) {
        Utilisateur utilisateur = utilisateurDAO.chercherParEmail(email).orElse(null);
        if (utilisateur == null) {
            throw new RessourceIntrouvableException("Utilisateur introuvable.");
        }
        utilisateur.setNom(demande.nom());
        utilisateur.setPrenom(demande.prenom());
        utilisateur.setTel(demande.tel());
        return enregistrer(utilisateur);
    }

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
