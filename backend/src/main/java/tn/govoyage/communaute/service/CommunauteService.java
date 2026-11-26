package tn.govoyage.communaute.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.govoyage.sejour.dao.SejourDAO;
import tn.govoyage.communaute.dto.PublicationRequest;
import static tn.govoyage.sejour.service.SejourService.*;

@Service
@Transactional(readOnly=true)
public class CommunauteService {
    private final SejourDAO dao;

    public CommunauteService(SejourDAO dao) { this.dao=dao; }

    public List<Map<String,Object>> lister(String recherche,Long compte) {
        if (recherche.length()>100) erreur("Recherche trop longue.");
        String filtre="%"+recherche.trim().toLowerCase(Locale.ROOT)+"%";
        return dao.lire("""
            SELECT p.*,u.prenom AS auteur,u.prenom AS auteur_prenom,u.nom AS auteur_nom,
            (SELECT COUNT(*) FROM appreciation a WHERE a.publication_id=p.id_p) AS likes,
            (SELECT COUNT(*) FROM avis a WHERE a.publication_id=p.id_p) AS avis,
            (SELECT COALESCE(AVG(CAST(a.note AS DECIMAL(4,2))),0) FROM avis a WHERE a.publication_id=p.id_p) AS note,
            (SELECT COUNT(*) FROM appreciation a WHERE a.publication_id=p.id_p AND a.utilisateur_id=?) AS aime
            FROM publication p JOIN utilisateur u ON u.id_u=p.utilisateur_id WHERE p.statut='PUBLIEE'
            AND (LOWER(p.ville) LIKE ? OR LOWER(p.pays) LIKE ?) ORDER BY p.cree_le DESC,p.id_p DESC LIMIT 100
            """, compte == null ? -1L : compte,filtre,filtre);
    }

    @Transactional
    public void publier(Long compte,PublicationRequest.Publication d) {
        String mediaUrl = nettoyer(d.mediaUrl());
        String mediaType = nettoyer(d.mediaType());
        verifierMedia(compte, mediaUrl, mediaType);
        dao.ecrire("INSERT INTO publication(utilisateur_id,titre,texte,pays,ville,media_url,media_type) VALUES(?,?,?,?,?,?,?)",
            compte,d.titre().trim(),d.texte().trim(),d.pays().trim(),d.ville().trim(),mediaUrl,mediaType);
    }

    @Transactional
    public void modifier(Long id,Long compte,PublicationRequest.Publication d) {
        unique(dao.lire("SELECT id_p FROM publication WHERE id_p=? AND utilisateur_id=? AND statut='PUBLIEE' FOR UPDATE",id,compte));
        String mediaUrl = nettoyer(d.mediaUrl());
        String mediaType = nettoyer(d.mediaType());
        verifierMedia(compte, mediaUrl, mediaType);
        dao.ecrire("UPDATE publication SET titre=?,texte=?,pays=?,ville=?,media_url=?,media_type=? WHERE id_p=?",
            d.titre().trim(),d.texte().trim(),d.pays().trim(),d.ville().trim(),mediaUrl,mediaType,id);
    }

    private String nettoyer(String valeur) {
        if (valeur == null || valeur.isBlank()) return null;
        return valeur.trim();
    }

    private void verifierMedia(Long compte, String mediaUrl, String mediaType) {
        if ((mediaUrl == null) != (mediaType == null)) erreur("La photo et son type doivent etre fournis ensemble.");
        if (mediaUrl == null) return;
        if (!mediaUrl.matches("/api/medias/[a-f0-9-]{36}")) erreur("Importez une photo ou une video depuis votre appareil.");
        String id = mediaUrl.substring("/api/medias/".length());
        Map<String,Object> media = unique(dao.lire("SELECT * FROM media_voyage WHERE id_m=? AND utilisateur_id=?", id, compte));
        boolean image = media.get("type_contenu").toString().startsWith("image/");
        if (image != "PHOTO".equals(mediaType)) erreur("Type de media incoherent.");
    }

    public List<Map<String,Object>> avis(Long id) {
        unique(dao.lire("SELECT id_p FROM publication WHERE id_p=? AND statut='PUBLIEE'",id));
        return dao.lire("""
            SELECT a.id_a,a.utilisateur_id,a.texte,a.note,a.cree_le,u.prenom AS auteur FROM avis a
            JOIN utilisateur u ON u.id_u=a.utilisateur_id WHERE publication_id=? ORDER BY a.cree_le DESC
            """,id);
    }

    public List<Map<String,Object>> likes(Long id) {
        unique(dao.lire("SELECT id_p FROM publication WHERE id_p=? AND statut='PUBLIEE'",id));
        return dao.lire("""
            SELECT u.prenom,u.nom FROM appreciation a JOIN utilisateur u ON u.id_u=a.utilisateur_id
            WHERE a.publication_id=? ORDER BY u.nom,u.prenom
            """,id);
    }

    @Transactional
    public void donnerAvis(Long id,Long compte,PublicationRequest.Avis d) {
        visible(id);
        dao.ecrire("DELETE FROM avis WHERE publication_id=? AND utilisateur_id=?",id,compte);
        dao.ecrire("INSERT INTO avis(publication_id,utilisateur_id,texte,note) VALUES(?,?,?,?)",
            id,compte,d.texte().trim(),d.note());
    }

    @Transactional
    public void aimer(Long id,Long compte,boolean actif) {
        visible(id);
        dao.ecrire("DELETE FROM appreciation WHERE publication_id=? AND utilisateur_id=?",id,compte);
        if (actif) dao.ecrire("INSERT INTO appreciation(publication_id,utilisateur_id) VALUES(?,?)",id,compte);
    }

    @Transactional
    public void signaler(Long id,Long compte,String motif) {
        visible(id);
        if (dao.compter("SELECT COUNT(*) FROM publication WHERE id_p=? AND utilisateur_id=?",id,compte)>0)
            erreur("Vous ne pouvez pas signaler votre propre recit.");
        if (dao.compter("SELECT COUNT(*) FROM signalement WHERE publication_id=? AND utilisateur_id=?",id,compte)==0)
            dao.ecrire("INSERT INTO signalement(publication_id,utilisateur_id,motif) VALUES(?,?,?)",id,compte,motif.trim());
    }

    @Transactional
    public void supprimer(Long id,Long compte) {
        unique(dao.lire("SELECT id_p FROM publication WHERE id_p=? AND utilisateur_id=? FOR UPDATE",id,compte));
        dao.ecrire("DELETE FROM publication WHERE id_p=?",id);
    }

    public List<Map<String,Object>> moderation() {
        return dao.lire("""
            SELECT p.*,u.prenom AS auteur,u.prenom AS auteur_prenom,u.nom AS auteur_nom,
            (SELECT COUNT(*) FROM signalement s WHERE s.publication_id=p.id_p AND s.statut='OUVERT') AS signalements
            FROM publication p JOIN utilisateur u ON u.id_u=p.utilisateur_id ORDER BY p.cree_le DESC
            """);
    }

    public List<Map<String,Object>> signalements(Long id) {
        return dao.lire("SELECT motif,statut,cree_le FROM signalement WHERE publication_id=? ORDER BY cree_le DESC",id);
    }

    @Transactional
    public void moderer(Long id,boolean visible) {
        unique(dao.lire("SELECT id_p FROM publication WHERE id_p=? FOR UPDATE",id));
        dao.ecrire("UPDATE publication SET statut=? WHERE id_p=?",visible?"PUBLIEE":"MASQUEE",id);
        dao.ecrire("UPDATE signalement SET statut='TRAITE' WHERE publication_id=?",id);
    }

    private void visible(Long id) {
        unique(dao.lire("SELECT id_p FROM publication WHERE id_p=? AND statut='PUBLIEE' FOR UPDATE",id));
    }
}
