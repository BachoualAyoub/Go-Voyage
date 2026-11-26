package tn.govoyage.exception;

import java.util.Map;

public record ErreurReponse(
        int statut,
        String message,
        Map<String, String> erreurs) {
}
