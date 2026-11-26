package tn.govoyage.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tn.govoyage.utilisateur.dao.UtilisateurDAO;
import tn.govoyage.utilisateur.entity.Role;
import tn.govoyage.utilisateur.entity.Utilisateur;

@Component
@Profile("dev")
@Order(1)
public class DonneesDev implements CommandLineRunner {

    private final UtilisateurDAO utilisateurDAO;

    private final PasswordEncoder passwordEncoder;

    public DonneesDev(UtilisateurDAO utilisateurDAO, PasswordEncoder passwordEncoder) {
        this.utilisateurDAO = utilisateurDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurDAO.count() != 0) {
            return;
        }
        creerCompte("Saidi", "Mohamed Rayen", "admin@gmail.com", "20 123 456", "admin1234", Role.ADMIN);
        creerCompte("Ksouri", "Mohamed Radhouen", "radhouen@gmail.com", "22 345 678", "radhouen123", Role.USER);
        creerCompte("Bachoual", "Ayoub", "ayoub@mail.tn", "25 456 789", "user12345", Role.USER);
        creerCompte("Ben Ali", "Sarra", "sarra@mail.tn", "50 987 654", "user12345", Role.USER);
        System.out.println("4 comptes de demonstration crees.");
    }

    private void creerCompte(String nom, String prenom, String email, String tel, String motDePasse, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email.toLowerCase());
        utilisateur.setPassword(passwordEncoder.encode(motDePasse));
        utilisateur.setTel(tel);
        utilisateur.setRole(role);
        utilisateurDAO.save(utilisateur);
    }
}
