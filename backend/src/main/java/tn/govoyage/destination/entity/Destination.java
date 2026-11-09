package tn.govoyage.destination.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Destination et prix journalier de reference du catalogue. */
@Entity
@Table(name = "destination")
public class Destination {
    @Id
    @Column(name = "id_d")
    private Long id;
    private String code;
    private String nom;
    private String ville;
    private String pays;
    private String region;
    private String ambiance;
    private int tarifJourTnd;
    private String accroche;
    private String description;

    /** Requis par JPA. */
    protected Destination() { }

    /** Retourne l'identifiant. */
    public Long getId() { return id; }
    /** Retourne le code visuel. */
    public String getCode() { return code; }
    /** Retourne le nom. */
    public String getNom() { return nom; }
    /** Retourne la ville. */
    public String getVille() { return ville; }
    /** Retourne le pays. */
    public String getPays() { return pays; }
    /** Retourne la region. */
    public String getRegion() { return region; }
    /** Retourne l'ambiance. */
    public String getAmbiance() { return ambiance; }
    /** Retourne le tarif indicatif en dinars. */
    public int getTarifJourTnd() { return tarifJourTnd; }
    /** Retourne le resume. */
    public String getAccroche() { return accroche; }
    /** Retourne la description. */
    public String getDescription() { return description; }
}
