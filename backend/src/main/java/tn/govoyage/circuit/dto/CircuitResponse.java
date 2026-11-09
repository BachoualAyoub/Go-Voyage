package tn.govoyage.circuit.dto;

import java.util.List;

/** Circuit ordonne et totaux calcules par le serveur. */
public record CircuitResponse(Long id, String nom, String description, boolean modele,
        Integer budgetTnd, int dureeJours, int estimationTnd, Long version,
        List<EtapeCircuitResponse> etapes) { }
