package tn.govoyage.exception;

/** Signale des identifiants de connexion incorrects (erreur 401). */
public class IdentifiantsInvalidesException extends RuntimeException {

    /** Message volontairement vague : on ne dit pas laquelle des deux donnees est fausse. */
    public IdentifiantsInvalidesException() {
        super("E-mail ou mot de passe incorrect.");
    }
}
