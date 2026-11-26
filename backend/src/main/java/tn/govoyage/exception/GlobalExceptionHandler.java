package tn.govoyage.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErreurReponse> gererIntegrite() {
        return ResponseEntity.status(409).body(new ErreurReponse(409,
            "Cette opération entre en conflit avec les données actuelles. Actualisez puis réessayez.", Map.of()));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErreurReponse> gererTaille() {
        return ResponseEntity.status(413).body(new ErreurReponse(413, "Le média est limité à 10 Mo.", Map.of()));
    }

    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<ErreurReponse> gererParametre() {
        return ResponseEntity.badRequest().body(new ErreurReponse(400, "Complétez tous les champs demandés.", Map.of()));
    }

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurReponse> gererValidation(MethodArgumentNotValidException exception) {
        Map<String, String> erreurs = new HashMap<String, String>();
        for (FieldError erreur : exception.getBindingResult().getFieldErrors()) {
            erreurs.put(erreur.getField(), erreur.getDefaultMessage());
        }
        ErreurReponse reponse = new ErreurReponse(400, "Les donnees envoyees sont invalides.", erreurs);
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ResponseEntity<ErreurReponse> gererEmailDejaUtilise(EmailDejaUtiliseException exception) {
        ErreurReponse reponse = new ErreurReponse(409, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ErreurReponse> gererIdentifiantsInvalides(IdentifiantsInvalidesException exception) {
        ErreurReponse reponse = new ErreurReponse(401, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurReponse> gererRessourceIntrouvable(RessourceIntrouvableException exception) {
        ErreurReponse reponse = new ErreurReponse(404, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OperationCompteException.class)
    public ResponseEntity<ErreurReponse> gererOperationCompte(OperationCompteException exception) {
        ErreurReponse reponse = new ErreurReponse(409, exception.getMessage(), Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErreurReponse> gererRequeteIllisible() {
        ErreurReponse reponse = new ErreurReponse(400, "Le format de la requete est invalide.", Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErreurReponse> gererStatut(ResponseStatusException exception) {
        ErreurReponse reponse = new ErreurReponse(exception.getStatusCode().value(), exception.getReason(), Map.of());
        return ResponseEntity.status(exception.getStatusCode()).body(reponse);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErreurReponse> gererConflit() {
        ErreurReponse reponse = new ErreurReponse(409, "Ce circuit a change. Rechargez la liste.", Map.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(reponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurReponse> gererErreurInconnue(Exception exception) {
        LOG.error("Erreur inattendue : ", exception);
        ErreurReponse reponse = new ErreurReponse(500, "Erreur interne du serveur.", Map.of());
        return new ResponseEntity<ErreurReponse>(reponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
