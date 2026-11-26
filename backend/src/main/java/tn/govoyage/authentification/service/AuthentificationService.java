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

@Service
public class AuthentificationService {

    private final UtilisateurDAO utilisateurDAO;

    private final PasswordEncoder passwordEncoder;

    private final JetonService jetonService;

    public AuthentificationService(UtilisateurDAO utilisateurDAO,
                                   PasswordEncoder passwordEncoder,
                                   JetonService jetonService) {
        this.utilisateurDAO = utilisateurDAO;
        this.passwordEncoder = passwordEncoder;
        this.jetonService = jetonService;
    }

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

    public UtilisateurResponse moi(String email) {
        Utilisateur utilisateur = utilisateurDAO.chercherParEmail(email).orElse(null);
        if (utilisateur == null) {
            throw new RessourceIntrouvableException("Utilisateur introuvable.");
        }
        return UtilisateurService.versReponse(utilisateur);
    }
}
