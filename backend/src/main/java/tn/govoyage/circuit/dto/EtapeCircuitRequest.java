package tn.govoyage.circuit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EtapeCircuitRequest(@NotNull @Positive Long destinationId, @Min(1) @Max(30) int jours) { }
