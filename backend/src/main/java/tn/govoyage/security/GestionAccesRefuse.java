package tn.govoyage.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Repond avec le JSON d'erreur 403 quand le role ne suffit pas. */
public class GestionAccesRefuse implements AccessDeniedHandler {

    /** Ecrit directement la reponse JSON sans dependre d'autres classes. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"statut\":403,\"message\":\"Acces refuse : votre role ne permet pas cette action.\",\"erreurs\":{}}");
    }
}
