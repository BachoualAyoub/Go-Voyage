package tn.govoyage.utilisateur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Coordonnees modifiables par le compte connecte, sans role ni mot de passe. */
public record ModificationProfilRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = 100) String prenom,
        @NotBlank(message = "Le telephone est obligatoire.")
        @Size(max = 20) @Pattern(regexp = "(?:[ ]*[0-9]){8,20}[ ]*",
                message = "Le telephone doit contenir au moins 8 chiffres.") String tel) {
}
