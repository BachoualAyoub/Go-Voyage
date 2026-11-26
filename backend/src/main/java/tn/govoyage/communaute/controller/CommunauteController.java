package tn.govoyage.communaute.controller;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tn.govoyage.security.CompteAuthentifie;
import tn.govoyage.communaute.dto.PublicationRequest;
import tn.govoyage.communaute.service.CommunauteService;

@RestController
@RequestMapping("/api")
public class CommunauteController {
    private final CommunauteService service;

    public CommunauteController(CommunauteService service) { this.service=service; }

    @GetMapping("/publications")
    public List<Map<String,Object>> lister(@RequestParam(defaultValue="") String recherche,@AuthenticationPrincipal CompteAuthentifie c) {
        return service.lister(recherche,c==null?null:c.id());
    }

    @PostMapping("/publications") @ResponseStatus(HttpStatus.CREATED)
    public void publier(@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Publication d) { service.publier(c.id(),d); }

    @PutMapping("/publications/{id}")
    public void modifier(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Publication d) { service.modifier(id,c.id(),d); }

    @GetMapping("/publications/{id}/avis")
    public List<Map<String,Object>> avis(@PathVariable Long id) { return service.avis(id); }

    @GetMapping("/publications/{id}/likes")
    public List<Map<String,Object>> likes(@PathVariable Long id) { return service.likes(id); }

    @PutMapping("/publications/{id}/avis")
    public void avis(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Avis d) { service.donnerAvis(id,c.id(),d); }

    @PutMapping("/publications/{id}/like")
    public void aimer(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Etat d) { service.aimer(id,c.id(),d.actif()); }

    @PostMapping("/publications/{id}/signalements")
    public void signaler(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody PublicationRequest.Signalement d) { service.signaler(id,c.id(),d.motif()); }

    @DeleteMapping("/publications/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id,@AuthenticationPrincipal CompteAuthentifie c) { service.supprimer(id,c.id()); }

    @GetMapping("/admin/publications")
    public List<Map<String,Object>> moderation() { return service.moderation(); }

    @GetMapping("/admin/publications/{id}/signalements")
    public List<Map<String,Object>> signalements(@PathVariable Long id) { return service.signalements(id); }

    @PutMapping("/admin/publications/{id}")
    public void moderer(@PathVariable Long id,@Valid @RequestBody PublicationRequest.Etat d) { service.moderer(id,d.actif()); }
}
