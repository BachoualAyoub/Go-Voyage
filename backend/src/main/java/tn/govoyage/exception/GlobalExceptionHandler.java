package tn.govoyage.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Intercepte toutes les erreurs de l'API et renvoie le format { statut, message, erreurs }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Journal de l'application. */
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erreurs de validation des champs (erreur 400). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurReponse> gererValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new HashMap<String, String>();
        for (FieldError erreur : exception.getBindingResult().getFieldErrors()) {
            erreurs.put(erreur.getField(), erreur.getDefaultMessage());
        }
        ErreurReponse reponse = new ErreurReponse(400, "Les donnees envoyees sont invalides.", erreurs);
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.BAD_REQUEST);
    }

    /** E-mail deja utilise (erreur 409). */
    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ResponseEntity<ErreurReponse> gererEmailDejaUtilise(EmailDejaUtiliseException exception) {
        ErreurReponse reponse = new ErreurReponse(409, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.CONFLICT);
    }

    /** Identifiants de connexion incorrects (erreur 401). */
    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ErreurReponse> gererIdentifiantsInvalides(IdentifiantsInvalidesException exception) {
        ErreurReponse reponse = new ErreurReponse(401, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.UNAUTHORIZED);
    }

    /** Ressource demandee introuvable (erreur 404). */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurReponse> gererRessourceIntrouvable(RessourceIntrouvableException exception) {
        ErreurReponse reponse = new ErreurReponse(404, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.NOT_FOUND);
    }

    /** Operation interdite sur son propre compte (erreur 409). */
    @ExceptionHandler(OperationCompteException.class)
    public ResponseEntity<ErreurReponse> gererOperationCompte(OperationCompteException exception) {
        ErreurReponse reponse = new ErreurReponse(409, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.CONFLICT);
    }

    /** Corps JSON ou parametre impossible a lire (erreur 400). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErreurReponse> gererRequeteIllisible() {
        ErreurReponse reponse = new ErreurReponse(400, "Le format de la requete est invalide.", Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.BAD_REQUEST);
    }

    /** Toute autre erreur non prevue (erreur 500). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurReponse> gererErreurInconnue(Exception exception) {
        LOG.error("Erreur inattendue : ", exception);
        ErreurReponse reponse = new ErreurReponse(500, "Erreur interne du serveur.", Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
