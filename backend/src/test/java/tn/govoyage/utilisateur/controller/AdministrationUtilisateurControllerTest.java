package tn.govoyage.utilisateur.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.entity.Role;
import tn.govoyage.utilisateur.entity.Utilisateur;

/** Verifie les regles d'acces et le cycle des comptes admin via l'API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdministrationUtilisateurControllerTest {
    private static final String ROUTE = "/api/admin/utilisateurs";
    private static final String MOT_DE_PASSE = "VoyageTest21";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UtilisateurDAO utilisateurs;
    @Autowired private PasswordEncoder hacheur;

    private Long idAdmin;
    private String jetonAdmin;

    /** Prepare un vrai administrateur dans la base de test. */
    @BeforeEach
    void preparer() throws Exception {
        Utilisateur admin = new Utilisateur();
        admin.setNom("Test");
        admin.setPrenom("Admin");
        admin.setEmail("admin-test@govoyage.test");
        admin.setTel("21 123 456");
        admin.setPassword(hacheur.encode(MOT_DE_PASSE));
        admin.setRole(Role.ADMIN);
        idAdmin = utilisateurs.saveAndFlush(admin).getId();
        jetonAdmin = connecter(admin.getEmail());
    }

    /** Un visiteur et un utilisateur simple ne peuvent pas gerer les comptes. */
    @Test
    void restreindreAdministration() throws Exception {
        mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content(compte("user-test@govoyage.test")))
                .andExpect(status().isCreated());
        mvc.perform(get(ROUTE).header("Authorization", connecter("user-test@govoyage.test")))
                .andExpect(status().isForbidden());
    }

    /** Le changement de role et la suppression invalident les autorisations du jeton. */
    @Test
    void gererCompteEtRevoquerAcces() throws Exception {
        String email = "admin-temp@govoyage.test";
        String corps = "{\"compte\":" + compte(email) + ",\"role\":\"ADMIN\"}";
        String reponse = mvc.perform(post(ROUTE).header("Authorization", jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(reponse).get("id").asLong();
        String ancienJeton = connecter(email);
        mvc.perform(get(ROUTE).header("Authorization", ancienJeton))
                .andExpect(status().isOk());

        modifierRole(id, email);
        mvc.perform(get(ROUTE).header("Authorization", ancienJeton))
                .andExpect(status().isForbidden());
        mvc.perform(delete(ROUTE + "/" + id).header("Authorization", jetonAdmin))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(compte(email)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/auth/moi").header("Authorization", ancienJeton))
                .andExpect(status().isUnauthorized());
    }

    /** Un administrateur ne peut pas supprimer son propre compte. */
    @Test
    void refuserAutoSuppression() throws Exception {
        mvc.perform(delete(ROUTE + "/" + idAdmin).header("Authorization", jetonAdmin))
                .andExpect(status().isConflict());
    }

    /** Met a jour le compte cible et verifie le role persiste. */
    private void modifierRole(long id, String email) throws Exception {
        String corps = """
                {"nom":"Modifie","prenom":"Voyage","email":"%s","tel":"22 333 444","role":"USER"}
                """.formatted(email);
        mvc.perform(put(ROUTE + "/" + id).header("Authorization", jetonAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Modifie"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    /** Ouvre une session par le chemin normal et renvoie l'en-tete Bearer. */
    private String connecter(String email) throws Exception {
        String demande = """
                {"email":"%s","motDePasse":"%s"}
                """.formatted(email, MOT_DE_PASSE);
        String reponse = mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON).content(demande))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(reponse).get("jeton").asText();
    }

    /** Retourne les donnees d'un compte de test. */
    private String compte(String email) {
        return """
                {"nom":"Test","prenom":"Voyage","email":"%s","tel":"21 123 456","motDePasse":"%s"}
                """.formatted(email, MOT_DE_PASSE);
    }
}
