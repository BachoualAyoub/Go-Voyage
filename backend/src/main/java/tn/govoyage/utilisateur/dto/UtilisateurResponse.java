package tn.govoyage.utilisateur.dto;

public record UtilisateurResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String tel,
        String role) {
}
