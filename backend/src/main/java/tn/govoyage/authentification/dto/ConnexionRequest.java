package tn.govoyage.authentification.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Donnees de connexion envoyees par le web ou le mobile. */
public record ConnexionRequest(
        @NotBlank(message = "L'e-mail est obligatoire.")
        @Email(message = "L'e-mail n'est pas valide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String motDePasse) {

    /** Evite de transmettre un mot de passe trop long a BCrypt. */
    public boolean isMotDePasseCompatible() {
        return motDePasse.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
