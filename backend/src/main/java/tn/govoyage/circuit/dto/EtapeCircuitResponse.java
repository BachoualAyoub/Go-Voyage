package tn.govoyage.circuit.dto;

/** Etape et estimation derivee du catalogue. */
public record EtapeCircuitResponse(Long destinationId, String nom, String ville, String ambiance,
        int jours, int tarifJourTnd, int coutTnd) { }
