package tn.govoyage.circuit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Choix d'une destination et d'une duree de sejour. */
public record EtapeCircuitRequest(@NotNull @Positive Long destinationId, @Min(1) @Max(30) int jours) { }
