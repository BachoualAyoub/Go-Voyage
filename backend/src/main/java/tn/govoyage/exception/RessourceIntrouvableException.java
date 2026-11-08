package tn.govoyage.exception;

/** Signale qu'une ressource demandee n'existe pas (erreur 404). */
public class RessourceIntrouvableException extends RuntimeException {

    /** Prend le message a afficher a l'utilisateur. */
    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
