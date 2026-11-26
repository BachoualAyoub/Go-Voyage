package tn.govoyage.sejour.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ParcoursVoyageTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test void reservationIsoleeEtIdempotente() throws Exception {
        String proprietaire=compte(false), autre=compte(false);
        String cle=UUID.randomUUID().toString();
        String demande=reservation(1,cle,1);
        JsonNode premiere=lire(envoyer("POST","/api/reservations",proprietaire,demande).andExpect(status().isCreated()));
        long id=premiere.get("id_r").asLong();
        assertEquals(360,premiere.get("montant").asLong());
        envoyer("POST","/api/reservations",proprietaire,demande).andExpect(status().isCreated()).andExpect(jsonPath("$.id_r").value(id));
        envoyer("POST","/api/reservations",proprietaire,reservation(1,cle,2)).andExpect(status().isConflict());
        mvc.perform(delete("/api/reservations/"+id).header("Authorization",autre)).andExpect(status().isNotFound());
        mvc.perform(get("/api/reservations").header("Authorization",autre)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/reservations/"+id).header("Authorization",proprietaire)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/reservations/"+id).header("Authorization",proprietaire)).andExpect(status().isNoContent());
    }

    @Test void protegeDisponibiliteEtTarif() throws Exception {
        String utilisateur=compte(false);
        jdbc.update("INSERT INTO hotel(destination_id,nom,ville,description,etoiles,tarif_nuit,chambres,capacite,image) VALUES(1,'Test stock','Tunis','Test',3,100,1,2,'sidi-bou-said')");
        long hotel=jdbc.queryForObject("SELECT MAX(id_h) FROM hotel WHERE nom='Test stock'",Long.class);
        String demande=reservation(hotel,UUID.randomUUID().toString(),1);
        envoyer("POST","/api/reservations",utilisateur,demande.replace("\"cle\"", "\"montantAttendu\":1,\"cle\"")).andExpect(status().isConflict());
        envoyer("POST","/api/reservations",utilisateur,demande).andExpect(status().isCreated());
        envoyer("POST","/api/reservations",utilisateur,reservation(hotel,UUID.randomUUID().toString(),1)).andExpect(status().isConflict());
        mvc.perform(get("/api/hotels").param("arrivee","2000-01-01").param("depart","2000-01-03")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
    }

    @Test void catalogueEtStatistiquesProteges() throws Exception {
        String utilisateur=compte(false), admin=compte(true);
        mvc.perform(get("/api/admin/statistiques").header("Authorization",utilisateur)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/statistiques").header("Authorization",admin)).andExpect(status().isOk()).andExpect(jsonPath("$.parVille").isArray());
        Map<String,Object> h=jdbc.queryForMap("SELECT * FROM hotel WHERE id_h=2");
        String d="""
            {"destinationId":2,"nom":"Les jardins du Cap","ville":"Hammamet","description":"Un séjour en bord de mer.",
            "etoiles":5,"tarifNuit":260,"chambres":12,"capacite":3,"image":"hammamet","actif":true,"version":%d}
            """.formatted(((Number)h.get("version")).longValue());
        envoyer("PUT","/api/admin/hotels/2",admin,d).andExpect(status().isOk());
        envoyer("PUT","/api/admin/hotels/2",admin,d).andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/destinations/1").header("Authorization",admin)).andExpect(status().isConflict());
    }

    @Test void communauteEtModeration() throws Exception {
        String auteur=compte(false), lecteur=compte(false), admin=compte(true);
        String titre="Recit-"+UUID.randomUUID();
        envoyer("POST","/api/publications",auteur,json.writeValueAsString(Map.of("titre",titre,"texte","Un souvenir tunisien.","pays","Tunisie","ville","Tozeur"))).andExpect(status().isCreated());
        long id=jdbc.queryForObject("SELECT id_p FROM publication WHERE titre=?",Long.class,titre);
        for(int i=0;i<2;i++) envoyer("PUT","/api/publications/"+id+"/like",lecteur,"{\"actif\":true}").andExpect(status().isOk());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM appreciation WHERE publication_id=?",Integer.class,id));
        envoyer("PUT","/api/publications/"+id+"/avis",lecteur,"{\"texte\":\"Superbe\",\"note\":5}").andExpect(status().isOk());
        envoyer("PUT","/api/publications/"+id+"/avis",lecteur,"{\"texte\":\"Mise à jour\",\"note\":4}").andExpect(status().isOk());
        mvc.perform(get("/api/publications/"+id+"/avis")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        envoyer("POST","/api/publications/"+id+"/signalements",lecteur,"{\"motif\":\"À vérifier\"}").andExpect(status().isOk());
        envoyer("PUT","/api/admin/publications/"+id,lecteur,"{\"actif\":false}").andExpect(status().isForbidden());
        envoyer("PUT","/api/admin/publications/"+id,admin,"{\"actif\":false}").andExpect(status().isOk());
        mvc.perform(get("/api/publications/"+id+"/avis")).andExpect(status().isNotFound());
        envoyer("PUT","/api/publications/"+id+"/like",lecteur,"{\"actif\":true}").andExpect(status().isNotFound());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM signalement WHERE publication_id=? AND statut='OUVERT'",Integer.class,id));
        mvc.perform(delete("/api/publications/"+id).header("Authorization",lecteur)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/publications/"+id).header("Authorization",auteur)).andExpect(status().isNoContent());
    }

    @Test void securiseLeMedia() throws Exception {
        String auteur=compte(false), autre=compte(false);
        MockMultipartFile photo=new MockMultipartFile("fichier","image.png","image/png",Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a6GkAAAAASUVORK5CYII="));
        JsonNode resultat=lire(mvc.perform(multipart("/api/medias").file(photo).header("Authorization",auteur)).andExpect(status().isCreated()));
        String url=resultat.get("url").asText();
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).header("Authorization",autre)).andExpect(status().isNotFound());
        mvc.perform(get(url).header("Authorization",auteur)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        MockMultipartFile html=new MockMultipartFile("fichier","test.png","image/png","<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/api/medias").file(html).header("Authorization",auteur)).andExpect(status().isBadRequest());
    }

    @Test void compareLesVols() throws Exception {
        mvc.perform(get("/api/vols").param("depart","Paris").param("arrivee","Tunis").param("date",LocalDate.now().plusDays(7).toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/vols").param("depart","Paris").param("arrivee","Tunis").param("date","2000-01-01")).andExpect(status().isBadRequest());
    }

    private String compte(boolean admin) throws Exception {
        String email=UUID.randomUUID()+"@test.tn";
        String donnees=json.writeValueAsString(Map.of("nom","Test","prenom","Voyage","email",email,"tel","21555111","motDePasse","motdepasse1"));
        envoyer("POST","/api/auth/inscription","",donnees).andExpect(status().isCreated());
        if(admin) jdbc.update("UPDATE utilisateur SET role='ADMIN' WHERE email=?",email);
        JsonNode session=lire(envoyer("POST","/api/auth/connexion","",json.writeValueAsString(Map.of("email",email,"motDePasse","motdepasse1"))).andExpect(status().isOk()));
        return "Bearer "+session.get("jeton").asText();
    }

    private String reservation(long hotel,String cle,int chambres) throws Exception {
        return json.writeValueAsString(Map.of("hotelId",hotel,"arrivee",LocalDate.now().plusDays(30).toString(),"depart",LocalDate.now().plusDays(32).toString(),"voyageurs",2,"chambres",chambres,"cle",cle));
    }

    private ResultActions envoyer(String methode,String url,String jeton,String corps) throws Exception {
        return mvc.perform(request(org.springframework.http.HttpMethod.valueOf(methode),url).header("Authorization",jeton).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private JsonNode lire(ResultActions resultat) throws Exception { return json.readTree(resultat.andReturn().getResponse().getContentAsByteArray()); }
}
