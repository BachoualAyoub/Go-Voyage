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

    protected Circuit() { }

    public Circuit(Long proprietaireId) {
        this.proprietaireId = proprietaireId;
    }

    public void actualiser(String nom, Integer budgetTnd, List<EtapeCircuit> etapes) {
        this.nom = nom;
        this.budgetTnd = budgetTnd;
        this.etapes.clear();
        this.etapes.addAll(etapes);
    }

    public Long getId() { return id; }

    public Long getProprietaireId() { return proprietaireId; }

    public String getNom() { return nom; }

    public String getDescription() { return description; }

    public Integer getBudgetTnd() { return budgetTnd; }

    public Long getVersion() { return version; }

    public List<EtapeCircuit> getEtapes() { return etapes; }
}
