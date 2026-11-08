package tn.govoyage.utilisateur.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tn.govoyage.utilisateur.entity.Role;

/** Informations modifiables par un administrateur. */
public record ModificationCompteRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = 100) String nom,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = 100) String prenom,
        @NotBlank(message = "L'e-mail est obligatoire.") @Email @Size(max = 100) String email,
        @NotBlank(message = "Le telephone est obligatoire.")
        @Size(max = 20) @Pattern(regexp = "(?:[ ]*[0-9]){8,20}[ ]*",
                message = "Le telephone doit contenir au moins 8 chiffres.") String tel,
        @NotNull(message = "Le role est obligatoire.") Role role) {
}
