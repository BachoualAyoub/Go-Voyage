package tn.govoyage.communaute.dto;

import jakarta.validation.constraints.*;

/** Formulaires du livre d'or. */
public class PublicationRequest {

    /** Recit de voyage et media facultatif. */
    public record Publication(
            @NotBlank(message = "Le titre est obligatoire.") @Size(max = 120, message = "Le titre est limite a 120 caracteres.") String titre,
            @NotBlank(message = "Le recit est obligatoire.") @Size(max = 2000, message = "Le recit est limite a 2000 caracteres.") String texte,
            @NotBlank(message = "Le pays est obligatoire.") @Size(max = 70, message = "Le pays est limite a 70 caracteres.") String pays,
            @NotBlank(message = "La ville est obligatoire.") @Size(max = 100, message = "La ville est limite a 100 caracteres.") String ville,
            @Size(max = 600) String mediaUrl,
            @Pattern(regexp = "PHOTO|VIDEO", message = "Type de media invalide.") String mediaType) { }

    /** Avis unique par voyageur et publication. */
    public record Avis(
            @NotBlank(message = "L'avis est obligatoire.") @Size(max = 1000, message = "L'avis est limite a 1000 caracteres.") String texte,
            @NotNull(message = "La note est obligatoire.") @Min(1) @Max(5) Integer note) { }

    /** Motif transmis a la moderation. */
    public record Signalement(
            @NotBlank(message = "Le motif est obligatoire.") @Size(max = 500, message = "Le motif est limite a 500 caracteres.") String motif) { }

    /** Etat explicite pour des interactions rejouables. */
    public record Etat(@NotNull(message = "L'etat est obligatoire.") Boolean actif) { }
}
