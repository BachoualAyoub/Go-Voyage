package tn.govoyage.circuit.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import tn.govoyage.destination.entity.Destination;

@Embeddable
public class EtapeCircuit {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;
    private int jours;

    protected EtapeCircuit() { }

    public EtapeCircuit(Destination destination, int jours) {
        this.destination = destination;
        this.jours = jours;
    }

    public Destination getDestination() { return destination; }

    public int getJours() { return jours; }
}
