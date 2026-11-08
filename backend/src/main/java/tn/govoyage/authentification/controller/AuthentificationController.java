package tn.govoyage.authentification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import tn.govoyage.authentification.dto.ConnexionRequest;
import tn.govoyage.authentification.dto.ConnexionResponse;
import tn.govoyage.authentification.service.AuthentificationService;
import tn.govoyage.utilisateur.dto.InscriptionRequest;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.service.UtilisateurService;

/** Endpoints d'authentification : inscription, connexion, compte courant. */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthentificationController {

    /** Service des comptes utilisateurs. */
    private final UtilisateurService utilisateurService;

    /** Service d'authentification. */
    private final AuthentificationService authentificationService;

    /** Prend les deux services. */
    public AuthentificationController(UtilisateurService utilisateurService,
                                      AuthentificationService authentificationService) {
        this.utilisateurService = utilisateurService;
        this.authentificationService = authentificationService;
    }

    /** Inscription d'un nouvel utilisateur (le role USER est force par le service). */
    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse inscrire(@Valid @RequestBody InscriptionRequest demande) {
        return utilisateurService.inscrire(demande);
    }

    /** Connexion : renvoie le jeton JWT et le compte connecte. */
    @PostMapping("/connexion")
    public ConnexionResponse connecter(@Valid @RequestBody ConnexionRequest demande) {
        return authentificationService.connecter(demande);
    }

    /** Renvoie le compte de l'utilisateur connecte (e-mail lu dans le jeton). */
    @GetMapping("/moi")
    @SecurityRequirement(name = "jeton")
    public UtilisateurResponse moi(Authentication authentication) {
        return authentificationService.moi(authentication.getName());
    }
}
                