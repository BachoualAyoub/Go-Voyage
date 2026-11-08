package tn.govoyage.utilisateur.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.govoyage.exception.EmailDejaUtiliseException;
import tn.govoyage.exception.OperationCompteException;
import tn.govoyage.exception.RessourceIntrouvableException;
import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.dto.CompteAdminRequest;
import tn.govoyage.utilisateur.dto.ModificationCompteRequest;
import tn.govoyage.utilisateur.dto.UtilisateurResponse;
import tn.govoyage.utilisateur.entity.Role;
import tn.govoyage.utilisateur.entity.Utilisateur;

/** Operations d'administration des comptes de GO Voyage. */
@Service
public class AdministrationUtilisateurService {

    private final UtilisateurDAO utilisateurDAO;
    private final UtilisateurService utilisateurService;

    /** Recoit le DAO et le service de creation des comptes. */
    public AdministrationUtilisateurService(UtilisateurDAO utilisateurDAO, UtilisateurService utilisateurService) {
        this.utilisateurDAO = utilisateurDAO;
        this.utilisateurService = utilisateurService;
    }

    /** Liste les comptes par identifiant sans exposer les mots de passe. */
    @Transactional(readOnly = true)
    public List<UtilisateurResponse> lister() {
        List<UtilisateurResponse> comptes = new ArrayList<>();
        for (Utilisateur compte : utilisateurDAO.findAll(Sort.by("id"))) {
            comptes.add(UtilisateurService.versReponse(compte));
        }
        return comptes;
    }

    /** Cree un compte dont le role est choisi par l'administrateur. */
    public UtilisateurResponse ajouter(CompteAdminRequest demande) {
        return utilisateurService.creer(demande.compte(), demande.role());
    }

    /** Modifie un compte tout en conservant son mot de passe. */
    @Transactional
    public UtilisateurResponse modifier(Long id, ModificationCompteRequest demande, Long idAdmin) {
        Utilisateur compte = chargerCompte(id);
        if (id.equals(idAdmin) && demande.role() != Role.ADMIN) {
            throw new OperationCompteException("Vous ne pouvez pas retirer votre propre role administrateur.");
        }
        String email = demande.email().toLowerCase(Locale.ROOT);
        if (!email.equals(compte.getEmail()) && utilisateurDAO.emailExiste(email)) {
            throw new EmailDejaUtiliseException();
        }
        compte.setNom(demande.nom());
        compte.setPrenom(demande.prenom());
        compte.setEmail(email);
        compte.setTel(demande.tel());
        compte.setRole(demande.role());
        return utilisateurService.enregistrer(compte);
    }

    /** Supprime un compte, sauf celui de l'administrateur connecte. */
    @Transactional
    public void supprimer(Long id, Long idAdmin) {
        Utilisateur compte = chargerCompte(id);
        if (id.equals(idAdmin)) {
            throw new OperationCompteException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        utilisateurDAO.delete(compte);
    }

    /** Verrouille le compte pendant sa modification ou sa suppression. */
    private Utilisateur chargerCompte(Long id) {
        Utilisateur compte = utilisateurDAO.verrouillerParId(id).orElse(null);
        if (compte == null) {
            throw new RessourceIntrouvableException("Utilisateur introuvable.");
        }
        return compte;
    }
}
