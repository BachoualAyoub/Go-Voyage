package tn.govoyage.security;

import java.security.Principal;

public record CompteAuthentifie(Long id, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
