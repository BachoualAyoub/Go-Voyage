package tn.govoyage.authentification.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import tn.govoyage.authentification.dto.ConnexionRequest;
import tn.govoyage.authentification.dto.ConnexionResponse;
import tn.govoyage.exception.IdentifiantsInvalidesException;
import tn.govoyage.exception.RessourceIntrouvableException;
import tn.govoyage.security.JetonService;
import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.entity.Utilisateur;
import tn.govoyage.utilisateur.service.UtilisateurService;

/** Logique d'authentification : verification des identifiants et jeton. */
@Service
public class AuthentificationService {

    /** DAO des utilisateurs. */
    private final UtilisateurDAO utilisateurDAO;

    /** Hacheur de mot de passe (BCrypt). */
    private final PasswordEncoder passwordEncoder;

    /** Service des jetons JWT. */
    private final JetonService jetonService;

    /** Prend le DAO, le hacheur de mot de passe et le service des jetons. */
    public AuthentificationService(UtilisateurDAO utilisateurDAO,
                                   PasswordEncoder passwordEncoder,
                                   JetonService jetonService) {
        this.utilisateurDAO = utilisateurDAO;
        this.passwordEncoder = passwordEncoder;
        this.jetonService = jetonService;
    }

    /** Verifie les identifiants puis renvoie le jeton et le compte connecte. */
    public ConnexionResponse connecter(ConnexionRequest demande) {
        String email = demande.email().toLowerCase(Locale.ROOT);
        Utilisateur utilisateur = utilisateurDAO.chercherParEmail(email).orElse(null);
        boolean motDePasseCorrect = utilisateur != null && demande.isMotDePasseCompatible()
                && passwordEncoder.matches(demande.motDePasse(), utilisateur.getPassword());
        if (!motDePasseCorrect) {
            throw new IdentifiantsInvalidesException();
        }
        String jeton = jetonService.genererJeton(utilisateur);
        return new ConnexionResponse(jeton, UtilisateurService.versReponse(utilisateur));
    }

    /** Renvoie le compte de l'utilisateur connecte (trouve a partir du jeton). */
    public UtilisateurResponse moi(String email) {
        Utilisateur utilisateur = utilisateurDAO.chercherParEmail(email).orElse(null);
        if (utilisateur == null) {
            throw new RessourceIntrouvableException("Utilisateur introuvable.");
        }
        return UtilisateurService.versReponse(utilisateur);
    }
}
