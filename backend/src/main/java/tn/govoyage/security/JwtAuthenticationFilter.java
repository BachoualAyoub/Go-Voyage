package tn.govoyage.security;

import java.io.IOException;
import java.util.List;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.entity.Utilisateur;

/** Filtre qui lit le jeton JWT de chaque requete et authentifie l'utilisateur. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Service des jetons JWT. */
    private final JetonService jetonService;
    private final UtilisateurDAO utilisateurDAO;

    /** Prend le service des jetons et le DAO des comptes. */
    public JwtAuthenticationFilter(JetonService jetonService, UtilisateurDAO utilisateurDAO) {
        this.jetonService = jetonService;
        this.utilisateurDAO = utilisateurDAO;
    }

    /** Lit l'en-tete "Authorization: Bearer ..." puis place l'utilisateur dans le contexte. */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chaine) throws ServletException, IOException {
        String entete = request.getHeader("Authorization");
        if (entete != null && entete.startsWith("Bearer ")) {
            Claims jeton = jetonService.lireJeton(entete.substring(7));
            if (jeton != null && jeton.get("id", Long.class) != null) {
                Utilisateur compte = utilisateurDAO.findById(jeton.get("id", Long.class)).orElse(null);
                if (compte != null && compte.getEmail().equals(jeton.getSubject())) {
                    placerUtilisateur(request, compte);
                }
            }
        }
        chaine.doFilter(request, response);
    }

    /** Applique le role actuel du compte, y compris apres une modification par un administrateur. */
    private void placerUtilisateur(HttpServletRequest request, Utilisateur compte) {
        SimpleGrantedAuthority autorite = new SimpleGrantedAuthority("ROLE_" + compte.getRole().name());
        CompteAuthentifie identite = new CompteAuthentifie(compte.getId(), compte.getEmail());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(identite, null, List.of(autorite));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
