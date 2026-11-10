package tn.govoyage.circuit.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import tn.govoyage.destination.entity.Destination;

/** Etape ordonnee d'un circuit. */
@Embeddable
public class EtapeCircuit {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;
    private int jours;

    /** Requis par JPA. */
    protected EtapeCircuit() { }

    /** Associe une destination a une duree. */
    public EtapeCircuit(Destination destination, int jours) {
        this.destination = destination;
        this.jours = jours;
    }

    /** Retourne la destination. */
    public Destination getDestination() { return destination; }

    /** Retourne la duree. */
    public int getJours() { return jours; }
}
