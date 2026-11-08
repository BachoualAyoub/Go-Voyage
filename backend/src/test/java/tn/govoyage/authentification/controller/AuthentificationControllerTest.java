package tn.govoyage.authentification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Tests d'integration des endpoints d'authentification (base H2 en memoire). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthentificationControllerTest {

    /** Outil pour envoyer des requetes HTTP au controller. */
    @Autowired
    private MockMvc mockMvc;

    /** Outil pour lire le JSON des reponses. */
    @Autowired
    private ObjectMapper objectMapper;

    /** Test : l'inscription avec des donnees completes cree le compte (201). */
    @Test
    public void inscriptionReussie() throws Exception {
        String corps = """
                {"nom":"Ben Salah","prenom":"Wassim","email":"wassim@gmail.com",
                 "tel":"21 555 111","motDePasse":"motdepasse1"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nom", org.hamcrest.Matchers.is("Ben Salah")))
                .andExpect(jsonPath("$.prenom", org.hamcrest.Matchers.is("Wassim")))
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is("wassim@gmail.com")))
                .andExpect(jsonPath("$.tel", org.hamcrest.Matchers.is("21 555 111")))
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /** Test : refuser a renvoyer un mot de passe meme si le JSON en contient un. */
    @Test
    public void inscriptionIgnoreRoleEtPassword() throws Exception {
        String corps = """
                {"nom":"Passe","prenom":"En Force","email":"force.user@gmail.com",
                 "tel":"20 777 888","motDePasse":"motdepasse1","role":"ADMIN","password":"pirate"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /** Test : un e-mail deja utilise est refuse avec l'erreur 409. */
    @Test
    public void inscriptionEmailDejaUtilise() throws Exception {
        String corps = """
                {"nom":"Doublon","prenom":"Test","email":"doublon@gmail.com",
                 "tel":"21 111 222","motDePasse":"motdepasse1"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statut", org.hamcrest.Matchers.is(409)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.is("Cet e-mail est deja utilise.")));
    }

    /** Test : l'e-mail est insensible a la casse (majuscules acceptees a la connexion). */
    @Test
    public void inscriptionEmailInsensibleALaCasse() throws Exception {
        String corps = """
                {"nom":"Casse","prenom":"Test","email":"CASSER@gmail.com",
                 "tel":"21 333 444","motDePasse":"motdepasse1"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is("casser@gmail.com")));
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"casser@gmail.com","motDePasse":"motdepasse1"}
                                """))
                .andExpect(status().isOk());
    }

    /** Test : des champs invalides sont refuses avec 400 et la liste des erreurs par champ. */
    @Test
    public void inscriptionChampsInvalides() throws Exception {
        String corps = """
                {"nom":"","prenom":"Test","email":"pas-un-email",
                 "tel":"123","motDePasse":"123"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statut", org.hamcrest.Matchers.is(400)))
                .andExpect(jsonPath("$.erreurs.nom").exists())
                .andExpect(jsonPath("$.erreurs.email").exists())
                .andExpect(jsonPath("$.erreurs.tel").exists())
                .andExpect(jsonPath("$.erreurs.motDePasse").exists());
    }

    /** Test : la connexion avec un compte existant renvoie un jeton et l'utilisateur (200). */
    @Test
    public void connexionReussie() throws Exception {
        String corps = """
                {"nom":"Mabrouk","prenom":"Chaima","email":"chai@gmail.com",
                 "tel":"50 111 222","motDePasse":"motdepasse1"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"chai@gmail.com","motDePasse":"motdepasse1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").exists())
                .andExpect(jsonPath("$.utilisateur.email", org.hamcrest.Matchers.is("chai@gmail.com")))
                .andExpect(jsonPath("$.utilisateur.password").doesNotExist());
    }

    /** Test : un mauvais mot de passe donne l'erreur 401. */
    @Test
    public void connexionMauvaisMotDePasse() throws Exception {
        String corps = """
                {"nom":"Trabelsi","prenom":"Nour","email":"nourtrab@gmail.com",
                 "tel":"90 111 222","motDePasse":"motdepasse1"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nourtrab@gmail.com","motDePasse":"mauvais-mot"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statut", org.hamcrest.Matchers.is(401)))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.is("E-mail ou mot de passe incorrect.")));
    }

    /** Test : un e-mail inconnu donne egalement l'erreur 401. */
    @Test
    public void connexionEmailInconnu() throws Exception {
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inconnu@gmail.com","motDePasse":"motdepasse1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statut", org.hamcrest.Matchers.is(401)));
    }

    /** Test : /moi renvoie le compte courant si le jeton envoye est valide (200). */
    @Test
    public void moiAvecJeton() throws Exception {
        String corps = """
                {"nom":"Jelassi","prenom":"Rania","email":"rania@gmail.com",
                 "tel":"22 888 999","motDePasse":"motdepasse1"}
                """;
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated());

        MvcResult resultatConnexion = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rania@gmail.com","motDePasse":"motdepasse1"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resultatConnexion.getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        String jeton = json.get("jeton").asText();

        mockMvc.perform(get("/api/auth/moi")
                        .header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is("rania@gmail.com")))
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /** Test : /moi sans jeton donne l'erreur 401 et un JSON d'erreur. */
    @Test
    public void moiSansJeton() throws Exception {
        mockMvc.perform(get("/api/auth/moi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statut", org.hamcrest.Matchers.is(401)))
                .andExpect(jsonPath("$.message").exists());
    }

    /** Test : un jeton invente (fausse signature) donne l'erreur 401 sur /moi. */
    @Test
    public void moiAvecJetonInvalide() throws Exception {
        mockMvc.perform(get("/api/auth/moi")
                        .header("Authorization", "Bearer jeton-invente"))
                .andExpect(status().isUnauthorized());
    }
}
