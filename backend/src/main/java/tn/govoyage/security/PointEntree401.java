package tn.govoyage.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Repond avec le JSON d'erreur 401 quand aucune authentification n'est fournie. */
public class PointEntree401 implements AuthenticationEntryPoint {

    /** Ecrit directement la reponse JSON sans dependre d'autres classes. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"statut\":401,\"message\":\"Authentification requise. Veuillez vous connecter.\",\"erreurs\":{}}");
    }
}
