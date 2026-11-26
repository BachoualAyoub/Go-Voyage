package tn.govoyage.exception;

public class EmailDejaUtiliseException extends RuntimeException {

    public EmailDejaUtiliseException() {
        super("Cet e-mail est deja utilise.");
    }
}
