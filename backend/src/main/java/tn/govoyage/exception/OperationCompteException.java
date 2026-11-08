package tn.govoyage.exception;

/** Operation interdite sur le compte de l'administrateur connecte. */
public class OperationCompteException extends RuntimeException {

    /** Conserve le message a afficher a l'administrateur. */
    public OperationCompteException(String message) {
        super(message);
    }
}
