package tn.govoyage.circuit.dto;

import java.util.List;

public record CircuitResponse(Long id, String nom, String description, boolean modele,
        Integer budgetTnd, int dureeJours, int estimationTnd, Long version,
        List<EtapeCircuitResponse> etapes) { }
