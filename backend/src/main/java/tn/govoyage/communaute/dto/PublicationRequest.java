package tn.govoyage.communaute.dto;

import jakarta.validation.constraints.*;

/** Formulaires du livre d'or. */
public class PublicationRequest {
    /** Recit de voyage et media facultatif. */
    public record Publication(@NotBlank @Size(max=120) String titre,@NotBlank @Size(max=2000) String texte,
            @NotBlank @Size(max=70) String pays,@NotBlank @Size(max=100) String ville,
            @Size(max=600) String mediaUrl,@Pattern(regexp="PHOTO|VIDEO") String mediaType) { }
    /** Avis unique par voyageur et publication. */
    public record Avis(@NotBlank @Size(max=1000) String texte,@Min(1) @Max(5) int note) { }
    /** Motif transmis a la moderation. */
    public record Signalement(@NotBlank @Size(max=500) String motif) { }
    /** Etat explicite pour des interactions rejouables. */
    public record Etat(boolean actif) { }
}
