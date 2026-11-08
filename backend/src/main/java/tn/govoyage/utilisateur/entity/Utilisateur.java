package tn.govoyage.utilisateur.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Compte d'un utilisateur de la plateforme (table utilisateur). */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    /** Identifiant unique (colonne id_u, auto). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_u")
    private Long id;

    /** Nom de l'utilisateur. */
    @Column(nullable = false, length = 100)
    private String nom;

    /** Prénom de l'utilisateur. */
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    /** Adresse e-mail, unique, sert à la connexion (stockée en minuscules). */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Mot de passe haché avec BCrypt (jamais renvoyé à l'extérieur). */
    @Column(nullable = false, length = 100)
    private String password;

    /** Numéro de téléphone. */
    @Column(nullable = false, length = 20)
    private String tel;

    /** Rôle du compte : ADMIN ou USER. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Constructeur vide imposé par JPA. */
    public Utilisateur() {
    }

    /** Identifiant du compte. */
    public Long getId() {
        return id;
    }

    /** Change l'identifiant (géré par la base de données). */
    public void setId(Long id) {
        this.id = id;
    }

    /** Nom de l'utilisateur. */
    public String getNom() {
        return nom;
    }

    /** Change le nom. */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /** Prénom de l'utilisateur. */
    public String getPrenom() {
        return prenom;
    }

    /** Change le prénom. */
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    /** Adresse e-mail. */
    public String getEmail() {
        return email;
    }

    /** Change l'adresse e-mail. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Mot de passe haché. */
    public String getPassword() {
        return password;
    }

    /** Change le mot de passe (déjà haché). */
    public void setPassword(String password) {
        this.password = password;
    }

    /** Numéro de téléphone. */
    public String getTel() {
        return tel;
    }

    /** Change le numéro de téléphone. */
    public void setTel(String tel) {
        this.tel = tel;
    }

    /** Rôle du compte. */
    public Role getRole() {
        return role;
    }

    /** Change le rôle du compte. */
    public void setRole(Role role) {
        this.role = role;
    }
}
