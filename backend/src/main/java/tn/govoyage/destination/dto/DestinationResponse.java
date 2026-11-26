package tn.govoyage.destination.dto;

public record DestinationResponse(Long id, String code, String nom, String ville,
        String pays, String region, String ambiance, int tarifJourTnd,
        String accroche, String description) { }
