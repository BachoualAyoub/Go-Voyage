package tn.govoyage.destination.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected Destination() { }

    public Long getId() { return id; }

    public String getCode() { return code; }

    public String getNom() { return nom; }

    public String getVille() { return ville; }

    public String getPays() { return pays; }

    public String getRegion() { return region; }

    public String getAmbiance() { return ambiance; }

    public int getTarifJourTnd() { return tarifJourTnd; }

    public String getAccroche() { return accroche; }

    public String getDescription() { return description; }
}
