package tn.govoyage.config;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tn.govoyage.sejour.dao.SejourDAO;

/** Cree des recits et des photos de demonstration pour le livre d'or (profil dev). */
@Component
@Profile("dev")
@Order(2)
public class DonneesLivreOrDev implements CommandLineRunner {

    /** Acces JDBC partage avec les autres services. */
    private final SejourDAO dao;

    /** Recoit l'acces aux donnees. */
    public DonneesLivreOrDev(SejourDAO dao) {
        this.dao = dao;
    }

    /** Remplace les photos rouges existing et remplit le livre d'or vide. */
    @Override
    @Transactional
    public void run(String... args) throws IOException {
        remplacerPhoto("11111111-1111-4111-8111-111111111111", "images/djerba.jpg");
        remplacerPhoto("22222222-2222-4222-8222-222222222222", "images/tozeur.jpg");
        corrigerTextes();
        if (dao.compter("SELECT COUNT(*) FROM publication") != 0) {
            return;
        }
        creerRecits();
        System.out.println("Recits du livre d'or crees.");
    }

    /** Inserent trois recits avec trois vraies photos du catalogue. */
    private void creerRecits() throws IOException {
        long ayoub = idUtilisateur("ayoub@mail.tn");
        long radhouen = idUtilisateur("radhouen@gmail.com");
        long sarra = idUtilisateur("sarra@mail.tn");
        enregistrerMedia("11111111-1111-4111-8111-111111111111", ayoub, "images/djerba.jpg");
        enregistrerMedia("22222222-2222-4222-8222-222222222222", radhouen, "images/tozeur.jpg");
        enregistrerMedia("33333333-3333-4333-8333-333333333333", sarra, "images/sidi-bou-said.jpg");
        creerPublication(1, ayoub, "Une semaine a Djerba",
            "L ilot d El Ghriba, les plages de Sidi Mahrez et les marches de Houmt Souk.", "Houmt Souk", "11111111-1111-4111-8111-111111111111");
        creerPublication(2, radhouen, "Les dunes de Tozeur",
            "Une nuit dans la palmeraie, le paysage lunaire du Chott el Djerid et le silence du desert.", "Tozeur", "22222222-2222-4222-8222-222222222222");
        creerPublication(3, sarra, "Sidi Bou Said en novembre",
            "Les ruelles bleues et blanches, le cafe des Nattes et le coucher de soleil sur la baie.", "Sidi Bou Said", "33333333-3333-4333-8333-333333333333");
        dao.ecrire("INSERT INTO avis(publication_id,utilisateur_id,texte,note) VALUES(?,?,?,?)",
            1, radhouen, "Une belle halte au bord de la mer.", 5);
        dao.ecrire("INSERT INTO avis(publication_id,utilisateur_id,texte,note) VALUES(?,?,?,?)",
            2, sarra, "Le desert reste inoubliable.", 4);
    }

    /** Remplace uniquement les petits medias de demonstration par une vraie photo. */
    private void remplacerPhoto(String id, String chemin) throws IOException {
        dao.ecrire("UPDATE media_voyage SET contenu=?, type_contenu='image/jpeg' WHERE id_m=? AND length(contenu)<1000",
            lireImage(chemin), id);
    }

    /** Corrige les textes de demonstration encodes avec un mauvais accent. */
    private void corrigerTextes() {
        dao.ecrire("UPDATE publication SET texte=? WHERE id_p=1",
            "L ilot d El Ghriba, les plages de Sidi Mahrez et les marches de Houmt Souk : un sejour tout en douceur.");
        dao.ecrire("UPDATE publication SET texte=? WHERE id_p=2",
            "Une nuit dans la palmeraie, le paysage lunaire du Chott el Djerid et le silence du desert.");
        dao.ecrire("UPDATE publication SET texte=? WHERE id_p=3",
            "Les ruelles bleues et blanches, le cafe des Nattes et le coucher de soleil sur la baie de Carthage.");
    }

    /** Enregistre une photo de demonstration sans ecraser un media deja present. */
    private void enregistrerMedia(String id, long utilisateur, String chemin) throws IOException {
        dao.ecrire("INSERT INTO media_voyage(id_m,utilisateur_id,type_contenu,contenu) VALUES(?,?,?,?) ON CONFLICT(id_m) DO NOTHING",
            id, utilisateur, "image/jpeg", lireImage(chemin));
    }

    /** Lit une image fournie dans les ressources du backend. */
    private byte[] lireImage(String chemin) throws IOException {
        return new ClassPathResource(chemin).getInputStream().readAllBytes();
    }

    /** Retrouve l'identifiant d'un compte de demonstration. */
    private long idUtilisateur(String email) {
        return ((Number) dao.lire("SELECT id_u FROM utilisateur WHERE email=?", email).get(0).get("id_u")).longValue();
    }

    /** Cree un recit publie avec une photo du catalogue. */
    private void creerPublication(long id, long utilisateur, String titre, String texte, String ville, String media) {
        dao.ecrire("INSERT INTO publication(id_p,utilisateur_id,titre,texte,pays,ville,media_url,media_type) VALUES(?,?,?,?,?,?,?,'PHOTO')",
            id, utilisateur, titre, texte, "Tunisie", ville, "/api/medias/" + media);
    }
}
