package tn.govoyage.utilisateur.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import tn.govoyage.utilisateur.entity.Role;

/** Donnees d'un nouveau compte cree par un administrateur. */
public record CompteAdminRequest(
        @Valid @NotNull(message = "Les informations du compte sont obligatoires.") InscriptionRequest compte,
        @NotNull(message = "Le role est obligatoire.") Role role) {
}
