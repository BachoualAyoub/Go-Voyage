package tn.govoyage.sejour.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.govoyage.sejour.dao.SejourDAO;
import tn.govoyage.sejour.dto.SejourRequest;

@Service
@Transactional(readOnly=true)
public class SejourService {
    private final SejourDAO dao;
    private static final String RESERVATIONS = """
        SELECT r.*, h.nom AS hotel_nom, h.ville, h.image FROM reservation r
        JOIN hotel h ON h.id_h=r.hotel_id
        """;

    public SejourService(SejourDAO dao) { this.dao = dao; }

    public List<Map<String,Object>> hotels(LocalDate arrivee, LocalDate depart, int voyageurs, int chambres) {
        dates(arrivee, depart);
        if (voyageurs < 1 || voyageurs > 30 || chambres < 1 || chambres > 10) erreur("Effectifs invalides.");
        List<Map<String,Object>> hotels = dao.lire("SELECT * FROM hotel WHERE actif=TRUE ORDER BY tarif_nuit,id_h");
        for (Map<String,Object> hotel : hotels) {
            int libres = disponibilite(hotel, arrivee, depart);
            hotel.put("disponibles", libres);
            hotel.put("reservable", libres >= chambres && nombre(hotel,"capacite") * chambres >= voyageurs);
            hotel.put("total", nombre(hotel,"tarif_nuit") * chambres * ChronoUnit.DAYS.between(arrivee, depart));
        }
        return hotels;
    }

    private int disponibilite(Map<String,Object> hotel, LocalDate arrivee, LocalDate depart) {
        List<Map<String,Object>> prises = dao.lire("""
            SELECT arrivee,depart,chambres FROM reservation WHERE hotel_id=? AND statut='CONFIRMEE'
            AND arrivee < ? AND depart > ?
            """, hotel.get("id_h"), depart, arrivee);
        long maximum = 0;
        for (LocalDate jour = arrivee; jour.isBefore(depart); jour = jour.plusDays(1)) {
            long utilise = 0;
            for (Map<String,Object> prise : prises) {
                LocalDate debut = LocalDate.parse(prise.get("arrivee").toString());
                LocalDate fin = LocalDate.parse(prise.get("depart").toString());
                if (!jour.isBefore(debut) && jour.isBefore(fin)) utilise += nombre(prise,"chambres");
            }
            maximum = Math.max(maximum, utilise);
        }
        return (int)(nombre(hotel,"chambres") - maximum);
    }

    @Transactional
    public Map<String,Object> reserver(Long compte, SejourRequest.Reservation demande) {
        dates(demande.arrivee(), demande.depart());
        Map<String,Object> hotel = unique(dao.lire("SELECT * FROM hotel WHERE id_h=? FOR UPDATE", demande.hotelId()));
        List<Map<String,Object>> precedente = dao.lire(RESERVATIONS + " WHERE r.utilisateur_id=? AND r.cle=?", compte, demande.cle().toString());
        if (!precedente.isEmpty()) return memeReservation(precedente.getFirst(), demande);
        if (!Boolean.TRUE.equals(hotel.get("actif"))) conflit("Cet hôtel est indisponible.");
        if (demande.voyageurs() > nombre(hotel,"capacite") * demande.chambres()) erreur("La capacité des chambres est insuffisante.");
        if (disponibilite(hotel, demande.arrivee(), demande.depart()) < demande.chambres()) conflit("Les chambres viennent d’être réservées. Changez vos dates.");
        long montant = nombre(hotel,"tarif_nuit") * demande.chambres() * ChronoUnit.DAYS.between(demande.arrivee(), demande.depart());
        if (demande.montantAttendu()!=null && demande.montantAttendu()!=montant)
            conflit("Le tarif a changé. Actualisez les offres avant de confirmer.");
        dao.ecrire("""
            INSERT INTO reservation(utilisateur_id,hotel_id,arrivee,depart,voyageurs,chambres,tarif_nuit,montant,cle)
            VALUES(?,?,?,?,?,?,?,?,?)
            """, compte, demande.hotelId(), demande.arrivee(), demande.depart(), demande.voyageurs(),
            demande.chambres(), hotel.get("tarif_nuit"), montant, demande.cle().toString());
        return unique(dao.lire(RESERVATIONS + " WHERE r.utilisateur_id=? AND r.cle=?", compte, demande.cle().toString()));
    }

    private Map<String,Object> memeReservation(Map<String,Object> ligne, SejourRequest.Reservation d) {
        if (nombre(ligne,"hotel_id") != d.hotelId() || !ligne.get("arrivee").toString().equals(d.arrivee().toString())
                || !ligne.get("depart").toString().equals(d.depart().toString()) || nombre(ligne,"chambres") != d.chambres()
                || nombre(ligne,"voyageurs") != d.voyageurs()) conflit("Cette clé désigne une autre réservation.");
        return ligne;
    }

    public List<Map<String,Object>> reservations(Long compte) {
        return dao.lire(RESERVATIONS + " WHERE r.utilisateur_id=? ORDER BY r.cree_le DESC,r.id_r DESC", compte);
    }

    @Transactional
    public void annuler(Long compte, Long id) {
        Map<String,Object> reservation = unique(dao.lire("SELECT * FROM reservation WHERE id_r=? AND utilisateur_id=? FOR UPDATE",id,compte));
        if ("ANNULEE".equals(reservation.get("statut"))) return;
        if (!LocalDate.parse(reservation.get("arrivee").toString()).isAfter(LocalDate.now())) conflit("Le séjour a déjà commencé.");
        dao.ecrire("UPDATE reservation SET statut='ANNULEE' WHERE id_r=?",id);
    }

    public List<Map<String,Object>> vols(String depart, String arrivee, LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now()) || date.isAfter(LocalDate.now().plusYears(1))) erreur("Choisissez une date dans les douze prochains mois.");
        if (depart.length() > 70 || arrivee.length() > 70) erreur("Ville invalide.");
        return dao.lire("SELECT * FROM vol WHERE LOWER(depart)=LOWER(?) AND LOWER(arrivee)=LOWER(?) ORDER BY prix,id_v",depart.trim(),arrivee.trim());
    }

    public static void dates(LocalDate arrivee, LocalDate depart) {
        if (arrivee == null || depart == null || arrivee.isBefore(LocalDate.now())
                || !depart.isAfter(arrivee) || ChronoUnit.DAYS.between(arrivee,depart) > 60
                || arrivee.isAfter(LocalDate.now().plusYears(1))) erreur("Sélectionnez un séjour de 1 à 60 nuits dans les douze prochains mois.");
    }

    public static Map<String,Object> unique(List<Map<String,Object>> lignes) {
        if (lignes.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ressource introuvable.");
        return lignes.getFirst();
    }

    public static long nombre(Map<String,Object> ligne, String cle) { return ((Number)ligne.get(cle)).longValue(); }

    public static void erreur(String texte) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,texte); }

    public static void conflit(String texte) { throw new ResponseStatusException(HttpStatus.CONFLICT,texte); }
}
