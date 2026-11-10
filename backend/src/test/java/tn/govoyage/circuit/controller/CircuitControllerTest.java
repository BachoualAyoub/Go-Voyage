package tn.govoyage.circuit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Verifie le catalogue, l'ordre des etapes et l'isolation entre comptes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CircuitControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    /** Verifie les modeles et le rejet des etapes hors du catalogue. */
    @Test
    void consulteLeCatalogueEtValideLesEtapes() throws Exception {
        mvc.perform(get("/api/destinations")).andExpect(status().isUnauthorized());
        String jeton = connecter("catalogue.sprint2@mail.tn");
        mvc.perform(get("/api/destinations").header("Authorization", jeton))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/api/circuits/modeles").header("Authorization", jeton))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estimationTnd").value(440));
        mvc.perform(delete("/api/circuits/1").header("Authorization", jeton))
                .andExpect(status().isNotFound());
        creer(jeton, """
                {"nom":"Destination inconnue","etapes":[{"destinationId":99999,"jours":2}]}
                """).andExpect(status().isBadRequest());
        creer(jeton, """
                {"nom":"Trop long","etapes":[{"destinationId":1,"jours":30},
                {"destinationId":2,"jours":30},{"destinationId":3,"jours":1}]}
                """).andExpect(status().isBadRequest());
    }

    /** Verifie la sauvegarde ordonnee et protege le circuit contre les modifications concurrentes. */
    @Test
    void gereUnCircuitPersonnelVersionne() throws Exception {
        String jeton = connecter("proprietaire.sprint2@mail.tn");
        String autreJeton = connecter("autre.sprint2@mail.tn");
        JsonNode circuit = lire(creer(jeton, """
                {"nom":"Mon voyage","budgetTnd":400,"etapes":[{"destinationId":1,"jours":2},
                {"destinationId":2,"jours":2}]}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.estimationTnd").value(440)));
        long id = circuit.get("id").asLong();
        String demande = """
                {"nom":"Mon voyage","budgetTnd":400,"version":%d,
                "etapes":[{"destinationId":2,"jours":1},{"destinationId":1,"jours":2}]}
                """.formatted(circuit.get("version").asLong());
        modifier(jeton, id, demande).andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeJours").value(3)).andExpect(jsonPath("$.estimationTnd").value(315));
        mvc.perform(get("/api/circuits/{id}", id).header("Authorization", jeton))
                .andExpect(status().isOk()).andExpect(jsonPath("$.etapes[0].destinationId").value(2));
        modifier(jeton, id, demande).andExpect(status().isConflict());
        verifierIsolation(autreJeton, id, demande);
        mvc.perform(delete("/api/circuits/{id}", id).header("Authorization", jeton)).andExpect(status().isNoContent());
        mvc.perform(get("/api/circuits/{id}", id).header("Authorization", jeton)).andExpect(status().isNotFound());
    }

    /** Le second compte ne peut ni voir ni changer le circuit. */
    private void verifierIsolation(String jeton, long id, String demande) throws Exception {
        mvc.perform(get("/api/circuits").header("Authorization", jeton)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/circuits/{id}", id).header("Authorization", jeton)).andExpect(status().isNotFound());
        modifier(jeton, id, demande).andExpect(status().isNotFound());
        mvc.perform(delete("/api/circuits/{id}", id).header("Authorization", jeton)).andExpect(status().isNotFound());
    }

    /** Envoie une creation avec le JWT du voyageur. */
    private ResultActions creer(String jeton, String demande) throws Exception {
        return mvc.perform(post("/api/circuits").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(demande));
    }

    /** Envoie une modification avec le JWT du voyageur. */
    private ResultActions modifier(String jeton, long id, String demande) throws Exception {
        return mvc.perform(put("/api/circuits/{id}", id).header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(demande));
    }

    /** Inscrit un compte de test et obtient son JWT par le flux normal. */
    private String connecter(String email) throws Exception {
        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content("""
                {"nom":"Voyage","prenom":"Test","email":"%s","tel":"21555111","motDePasse":"motdepasse1"}
                """.formatted(email))).andExpect(status().isCreated());
        JsonNode connexion = lire(mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","motDePasse":"motdepasse1"}
                        """.formatted(email))).andExpect(status().isOk()));
        return "Bearer " + connexion.get("jeton").asText();
    }

    /** Lit le JSON produit par le controleur. */
    private JsonNode lire(ResultActions reponse) throws Exception {
        return json.readTree(reponse.andReturn().getResponse().getContentAsByteArray());
    }
}
