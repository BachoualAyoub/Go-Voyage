package tn.govoyage.authentification.dto;

import tn.govoyage.utilisateur.dto.UtilisateurResponse;

/** Reponse de connexion : le jeton JWT et le compte connecte. */
public record ConnexionResponse(
        String jeton,
        UtilisateurResponse utilisateur) {
}
