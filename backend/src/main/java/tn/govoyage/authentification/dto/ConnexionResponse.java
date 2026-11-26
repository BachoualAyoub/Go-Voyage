package tn.govoyage.authentification.dto;

import tn.govoyage.utilisateur.dto.UtilisateurResponse;

public record ConnexionResponse(
        String jeton,
        UtilisateurResponse utilisateur) {
}
