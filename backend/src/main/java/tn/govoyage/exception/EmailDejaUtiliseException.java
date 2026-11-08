package tn.govoyage.exception;

/** Signale qu'un e-mail est deja utilise par un autre compte (erreur 409). */
public class EmailDejaUtiliseException extends RuntimeException {

    /** Message comprehensible pour l'utilisateur. */
    public EmailDejaUtiliseException() {
        super("Cet e-mail est deja utilise.");
    }
}
