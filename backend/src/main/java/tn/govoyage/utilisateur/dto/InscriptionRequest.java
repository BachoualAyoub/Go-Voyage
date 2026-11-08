package tn.govoyage.utilisateur.dto;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Donnees d'inscription envoyees par le web ou le mobile. */
public record InscriptionRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres.")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 100, message = "Le prenom ne doit pas depasser 100 caracteres.")
        String prenom,

        @NotBlank(message = "L'e-mail est obligatoire.")
        @Email(message = "L'e-mail n'est pas valide.")
        @Size(max = 100, message = "L'e-mail ne doit pas depasser 100 caracteres.")
        String email,

        @NotBlank(message = "Le telephone est obligatoire.")
        @Size(max = 20)
        @Pattern(regexp = "(?:[ ]*[0-9]){8,20}[ ]*", message = "Le telephone doit contenir au moins 8 chiffres.")
        String tel,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caracteres.")
        String motDePasse) {

    /** Limite en octets du hachage BCrypt. */
    @JsonIgnore
    @AssertTrue(message = "Le mot de passe ne doit pas depasser 72 octets.")
    public boolean isMotDePasseCompatible() {
        return motDePasse == null || motDePasse.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
