package tn.govoyage.circuit.entity;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Circuit d'un voyageur ou modele partage du catalogue. */
@Entity
@Table(name = "circuit")
public class Circuit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_c")
    private Long id;
    private Long proprietaireId;
    private String nom;
    private String description;
    private Integer budgetTnd;
    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "etape_circuit", joinColumns = @JoinColumn(name = "circuit_id"))
    @OrderColumn(name = "ordre")
    private List<EtapeCircuit> etapes = new ArrayList<>();

    /** Requis par JPA. */
    protected Circuit() { }

    /** Cree un circuit pour le compte authentifie. */
    public Circuit(Long proprietaireId) {
        this.proprietaireId = proprietaireId;
    }

    /** Remplace le plan en conservant l'ordre soumis. */
    public void actualiser(String nom, Integer budgetTnd, List<EtapeCircuit> etapes) {
        this.nom = nom;
        this.budgetTnd = budgetTnd;
        this.etapes.clear();
        this.etapes.addAll(etapes);
    }

    /** Retourne l'identifiant. */
    public Long getId() { return id; }
    /** Retourne le proprietaire, absent pour un modele. */
    public Long getProprietaireId() { return proprietaireId; }
    /** Retourne le nom. */
    public String getNom() { return nom; }
    /** Retourne le texte de presentation du modele. */
    public String getDescription() { return description; }
    /** Retourne le budget souhaite. */
    public Integer getBudgetTnd() { return budgetTnd; }
    /** Retourne la revision utilisee pour eviter les ecrasements. */
    public Long getVersion() { return version; }
    /** Retourne les etapes dans leur ordre. */
    public List<EtapeCircuit> getEtapes() { return etapes; }
}
