package tn.govoyage.security;

import java.security.Principal;

/** Identite d'un compte authentifie par le filtre JWT. */
public record CompteAuthentifie(Long id, String email) implements Principal {
    /** Renvoie l'e-mail attendu par Spring Security. */
    @Override
    public String getName() {
        return email;
    }
}
