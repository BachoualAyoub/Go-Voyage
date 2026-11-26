package tn.govoyage.authentification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import tn.govoyage.utilisateur.dto.ModificationProfilRequest;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.service.UtilisateurService;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthentificationController {

    private final UtilisateurService utilisateurService;

    private final AuthentificationService authentificationService;

    public AuthentificationController(UtilisateurService utilisateurService,
                                      AuthentificationService authentificationService) {
        this.utilisateurService = utilisateurService;
        this.authentificationService = authentificationService;
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse inscrire(@Valid @RequestBody InscriptionRequest demande) {
        return utilisateurService.inscrire(demande);
    }

    @PostMapping("/connexion")
    public ConnexionResponse connecter(@Valid @RequestBody ConnexionRequest demande) {
        return authentificationService.connecter(demande);
    }

    @GetMapping("/moi")
    @SecurityRequirement(name = "jeton")
    public UtilisateurResponse moi(Authentication authentication) {
        return authentificationService.moi(authentication.getName());
    }

    @PutMapping("/moi")
    @SecurityRequirement(name = "jeton")
    public UtilisateurResponse modifierProfil(Authentication authentication,
            @Valid @RequestBody ModificationProfilRequest demande) {
        return utilisateurService.modifierProfil(authentication.getName(), demande);
    }

    @PostMapping("/deconnexion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "jeton")
    public void deconnecter() {

    }
}
