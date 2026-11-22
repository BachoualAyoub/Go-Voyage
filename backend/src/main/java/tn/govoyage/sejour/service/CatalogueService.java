package tn.govoyage.sejour.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.govoyage.sejour.dao.SejourDAO;
import tn.govoyage.sejour.dto.SejourRequest;
import static tn.govoyage.sejour.service.SejourService.*;

/** Gestion administrative du catalogue et indicateurs issus des donnees reelles. */
@Service
@Transactional(readOnly=true)
public class CatalogueService {
    private final SejourDAO dao;
    /** Recoit les acces aux donnees. */
    public CatalogueService(SejourDAO dao) { this.dao = dao; }
    /** Liste aussi les hotels retires du catalogue public. */
    public List<Map<String,Object>> hotels() { return dao.lire("SELECT * FROM hotel ORDER BY id_h"); }
    /** Cree ou remplace un hotel avec controle de version. */
    @Transactional
    public void sauverHotel(Long id, SejourRequest.Hotel d) {
        unique(dao.lire("SELECT id_d FROM destination WHERE id_d=?",d.destinationId()));
        if (id == null) {
            dao.ecrire("""
                INSERT INTO hotel(destination_id,nom,ville,description,etoiles,tarif_nuit,chambres,capacite,image,actif)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """,d.destinationId(),d.nom().trim(),d.ville().trim(),d.description(),d.etoiles(),d.tarifNuit(),d.chambres(),d.capacite(),d.image(),d.actif());
        } else {
            Map<String,Object> hotel = unique(dao.lire("SELECT * FROM hotel WHERE id_h=? FOR UPDATE",id));
            if (d.version()==null || nombre(hotel,"version") != d.version()) conflit("Cet hôtel a changé. Rechargez le catalogue.");
            if (d.chambres() < nombre(hotel,"chambres") || d.capacite() < nombre(hotel,"capacite")) {
                if (dao.compter("SELECT COUNT(*) FROM reservation WHERE hotel_id=? AND statut='CONFIRMEE' AND depart>CURRENT_DATE",id)>0)
                    conflit("Des séjours sont confirmés. Conservez la capacité actuelle.");
            }
            dao.ecrire("""
                UPDATE hotel SET destination_id=?,nom=?,ville=?,description=?,etoiles=?,tarif_nuit=?,chambres=?,capacite=?,image=?,actif=?,version=version+1 WHERE id_h=?
                """,d.destinationId(),d.nom().trim(),d.ville().trim(),d.description(),d.etoiles(),d.tarifNuit(),d.chambres(),d.capacite(),d.image(),d.actif(),id);
        }
    }
    /** Retire un hotel de la vente en gardant l'historique des reservations. */
    @Transactional
    public void retirerHotel(Long id) {
        unique(dao.lire("SELECT id_h FROM hotel WHERE id_h=? FOR UPDATE",id));
        dao.ecrire("UPDATE hotel SET actif=FALSE,version=version+1 WHERE id_h=?",id);
    }
    /** Cree ou modifie une fiche destination du catalogue partage. */
    @Transactional
    public void sauverDestination(Long id, SejourRequest.Destination d) {
        List<Map<String,Object>> doublon = dao.lire("SELECT id_d FROM destination WHERE code=?",d.code());
        if (!doublon.isEmpty() && (id == null || nombre(doublon.getFirst(),"id_d") != id)) conflit("Ce code destination est déjà utilisé.");
        if (id == null) {
            dao.ecrire("""
                INSERT INTO destination(code,nom,ville,pays,region,ambiance,tarif_jour_tnd,accroche,description) VALUES(?,?,?,?,?,?,?,?,?)
                """,d.code(),d.nom(),d.ville(),d.pays(),d.region(),d.ambiance(),d.tarifJourTnd(),d.accroche(),d.description());
        } else {
            unique(dao.lire("SELECT id_d FROM destination WHERE id_d=? FOR UPDATE",id));
            dao.ecrire("""
                UPDATE destination SET code=?,nom=?,ville=?,pays=?,region=?,ambiance=?,tarif_jour_tnd=?,accroche=?,description=? WHERE id_d=?
                """,d.code(),d.nom(),d.ville(),d.pays(),d.region(),d.ambiance(),d.tarifJourTnd(),d.accroche(),d.description(),id);
        }
    }
    /** Supprime une destination si elle n'est utilisee par aucun parcours. */
    @Transactional
    public void supprimerDestination(Long id) {
        unique(dao.lire("SELECT id_d FROM destination WHERE id_d=? FOR UPDATE",id));
        if (dao.compter("SELECT COUNT(*) FROM etape_circuit WHERE destination_id=?",id)>0 ||
                dao.compter("SELECT COUNT(*) FROM hotel WHERE destination_id=?",id)>0) conflit("Cette destination est liée à un hôtel ou à un circuit.");
        dao.ecrire("DELETE FROM destination WHERE id_d=?",id);
    }
    /** Produit les indicateurs et series du tableau de bord. */
    public Map<String,Object> statistiques() {
        Map<String,Object> stats = new LinkedHashMap<>();
        stats.put("comptes",dao.compter("SELECT COUNT(*) FROM utilisateur"));
        stats.put("destinations",dao.compter("SELECT COUNT(*) FROM destination"));
        stats.put("hotels",dao.compter("SELECT COUNT(*) FROM hotel WHERE actif=TRUE"));
        stats.put("circuits",dao.compter("SELECT COUNT(*) FROM circuit WHERE proprietaire_id IS NOT NULL"));
        stats.put("reservations",dao.compter("SELECT COUNT(*) FROM reservation WHERE statut='CONFIRMEE'"));
        stats.put("montant",dao.compter("SELECT COALESCE(SUM(montant),0) FROM reservation WHERE statut='CONFIRMEE'"));
        stats.put("annulations",dao.compter("SELECT COUNT(*) FROM reservation WHERE statut='ANNULEE'"));
        stats.put("publications",dao.compter("SELECT COUNT(*) FROM publication WHERE statut='PUBLIEE'"));
        stats.put("signalements",dao.compter("SELECT COUNT(*) FROM signalement WHERE statut='OUVERT'"));
        stats.put("parVille",dao.lire("""
            SELECT h.ville,COUNT(*) AS reservations, SUM(r.montant) AS montant FROM reservation r JOIN hotel h ON h.id_h=r.hotel_id
            WHERE r.statut='CONFIRMEE' GROUP BY h.ville ORDER BY reservations DESC,h.ville
            """));
        stats.put("parJour",dao.lire("""
            SELECT CAST(cree_le AS DATE) AS jour,COUNT(*) AS nombre,SUM(montant) AS montant
            FROM reservation WHERE statut='CONFIRMEE' GROUP BY CAST(cree_le AS DATE) ORDER BY jour
            """));
        stats.put("recentes",dao.lire("""
            SELECT r.id_r,r.arrivee,r.depart,r.montant,r.statut,h.nom AS hotel_nom,u.prenom,u.nom
            FROM reservation r JOIN hotel h ON h.id_h=r.hotel_id JOIN utilisateur u ON u.id_u=r.utilisateur_id
            ORDER BY r.cree_le DESC,r.id_r DESC LIMIT 8
            """));
        return stats;
    }
}
