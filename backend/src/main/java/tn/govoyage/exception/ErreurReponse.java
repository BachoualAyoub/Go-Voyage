package tn.govoyage.exception;

import java.util.Map;

/** Format unique des erreurs de l'API : { statut, message, erreurs }. */
public record ErreurReponse(
        int statut,
        String message,
        Map<String, String> erreurs) {
}
