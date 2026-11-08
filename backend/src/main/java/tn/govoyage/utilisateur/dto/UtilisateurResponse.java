package tn.govoyage.utilisateur.dto;

/** Reponse renvoyee pour un compte utilisateur, sans le mot de passe. */
public record UtilisateurResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String tel,
        String role) {
}
