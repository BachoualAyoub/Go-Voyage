package tn.govoyage.circuit.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Formulaire de creation ou de modification d'un circuit. */
public record CircuitRequest(
        @NotBlank @Size(max = 90) String nom,
        @Min(1) @Max(1000000) Integer budgetTnd,
        @NotEmpty @Size(max = 10) List<@Valid @NotNull EtapeCircuitRequest> etapes,
        @PositiveOrZero Long version) { }
