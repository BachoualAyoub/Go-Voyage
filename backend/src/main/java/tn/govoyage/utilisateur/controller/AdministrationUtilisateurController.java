package tn.govoyage.utilisateur.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.govoyage.security.CompteAuthentifie;
import tn.govoyage.utilisateur.dto.CompteAdminRequest;
import tn.govoyage.utilisateur.dto.ModificationCompteRequest;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.service.AdministrationUtilisateurService;

/** Gestion des comptes, reservee au role ADMIN par Spring Security. */
@RestController
@RequestMapping("/api/admin/utilisateurs")
@SecurityRequirement(name = "jeton")
public class AdministrationUtilisateurController {

    private final AdministrationUtilisateurService administration;

    /** Recoit le service d'administration des comptes. */
    public AdministrationUtilisateurController(AdministrationUtilisateurService administration) {
        this.administration = administration;
    }

    /** Liste les comptes. */
    @GetMapping
    public List<UtilisateurResponse> lister() {
        return administration.lister();
    }

    /** Cree un compte avec le role choisi. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse ajouter(@Valid @RequestBody CompteAdminRequest demande) {
        return administration.ajouter(demande);
    }

    /** Met a jour un compte existant. */
    @PutMapping("/{id}")
    public UtilisateurResponse modifier(@PathVariable Long id, @Valid @RequestBody ModificationCompteRequest demande,
                                        @AuthenticationPrincipal CompteAuthentifie admin) {
        return administration.modifier(id, demande, admin.id());
    }

    /** Supprime un compte existant. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie admin) {
        administration.supprimer(id, admin.id());
    }
}
