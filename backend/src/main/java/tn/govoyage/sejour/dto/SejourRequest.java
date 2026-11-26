package tn.govoyage.sejour.dto;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.*;

public class SejourRequest {

    public record Reservation(@NotNull Long hotelId, @NotNull LocalDate arrivee,
            @NotNull LocalDate depart, @Min(1) @Max(30) int voyageurs,
            @Min(1) @Max(10) int chambres, @NotNull UUID cle, @Min(1) Long montantAttendu) { }

    public record Hotel(@NotNull Long destinationId, @NotBlank @Size(max=120) String nom,
            @NotBlank @Size(max=100) String ville, @NotBlank @Size(max=1200) String description,
            @Min(1) @Max(5) int etoiles, @Min(1) @Max(100000) int tarifNuit,
            @Min(1) @Max(500) int chambres, @Min(1) @Max(10) int capacite,
            @NotBlank @Pattern(regexp="[a-z0-9-]{1,40}") String image, boolean actif, Long version) { }

    public record Destination(@NotBlank @Pattern(regexp="[a-z0-9-]{1,40}") String code,
            @NotBlank @Size(max=100) String nom, @NotBlank @Size(max=100) String ville,
            @NotBlank @Size(max=70) String pays, @NotBlank @Size(max=100) String region,
            @Pattern(regexp="MER|CULTURE|NATURE") @NotNull String ambiance,
            @Min(1) @Max(100000) int tarifJourTnd, @NotBlank @Size(max=140) String accroche,
            @NotBlank @Size(max=750) String description) { }
}
