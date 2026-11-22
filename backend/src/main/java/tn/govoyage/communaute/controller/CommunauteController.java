package tn.govoyage.communaute.controller;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tn.govoyage.security.CompteAuthentifie;
import tn.govoyage.communaute.dto.PublicationRequest;
import tn.govoyage.communaute.service.CommunauteService;

/** Routes du livre d'or et de sa moderation. */
@RestController
@RequestMapping("/api")
public class CommunauteController {
    private final CommunauteService service;
    /** Recoit la logique communautaire. */
    public CommunauteController(CommunauteService service) { this.service=service; }
    /** Catalogue public avec etat du like du compte courant. */
    @GetMapping("/publications")
    public List<Map<String,Object>> lister(@RequestParam(defaultValue="") String recherche,@AuthenticationPrincipal CompteAuthentifie c) {
        return service.lister(recherche,c==null?null:c.id());
    }
    /** Partage un recit. */
    @PostMapping("/publications") @ResponseStatus(HttpStatus.CREATED)
    public void publier(@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Publication d) { service.publier(c.id(),d); }
    /** Lit les commentaires et notes. */
    @GetMapping("/publications/{id}/avis")
    public List<Map<String,Object>> avis(@PathVariable Long id) { return service.avis(id); }
    /** Ecrit ou modifie son avis. */
    @PutMapping("/publications/{id}/avis")
    public void avis(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Avis d) { service.donnerAvis(id,c.id(),d); }
    /** Ajoute ou retire son like. */
    @PutMapping("/publications/{id}/like")
    public void aimer(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@RequestBody PublicationRequest.Etat d) { service.aimer(id,c.id(),d.actif()); }
    /** Transmet un signalement. */
    @PostMapping("/publications/{id}/signalements")
    public void signaler(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Signalement d) { service.signaler(id,c.id(),d.motif()); }
    /** Retire son propre recit. */
    @DeleteMapping("/publications/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c) { service.supprimer(id,c.id()); }
    /** File de moderation. */
    @GetMapping("/admin/publications")
    public List<Map<String,Object>> moderation() { return service.moderation(); }
    /** Motifs de signalement pour le moderateur. */
    @GetMapping("/admin/publications/{id}/signalements")
    public List<Map<String,Object>> signalements(@PathVariable Long id) { return service.signalements(id); }
    /** Statue sur un contenu signale. */
    @PutMapping("/admin/publications/{id}")
    public void moderer(@PathVariable Long id,@RequestBody PublicationRequest.Etat d) { service.moderer(id,d.actif()); }
}
