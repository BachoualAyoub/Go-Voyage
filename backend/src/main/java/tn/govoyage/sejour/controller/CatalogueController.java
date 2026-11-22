package tn.govoyage.sejour.controller;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import tn.govoyage.sejour.dto.SejourRequest;
import tn.govoyage.sejour.service.CatalogueService;

/** Administration protegee par le role ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class CatalogueController {
    private final CatalogueService service;
    /** Recoit les operations du catalogue. */
    public CatalogueController(CatalogueService service) { this.service=service; }
    /** Fournit les series du tableau de bord. */
    @GetMapping("/statistiques") public Map<String,Object> statistiques() { return service.statistiques(); }
    /** Inclut les hotels archives. */
    @GetMapping("/hotels") public List<Map<String,Object>> hotels() { return service.hotels(); }
    /** Ajoute un hotel. */
    @PostMapping("/hotels") @ResponseStatus(HttpStatus.CREATED)
    public void creerHotel(@Valid @RequestBody SejourRequest.Hotel d) { service.sauverHotel(null,d); }
    /** Modifie un hotel. */
    @PutMapping("/hotels/{id}")
    public void modifierHotel(@PathVariable Long id,@Valid @RequestBody SejourRequest.Hotel d) { service.sauverHotel(id,d); }
    /** Archive un hotel. */
    @DeleteMapping("/hotels/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirerHotel(@PathVariable Long id) { service.retirerHotel(id); }
    /** Ajoute une destination. */
    @PostMapping("/destinations") @ResponseStatus(HttpStatus.CREATED)
    public void creerDestination(@Valid @RequestBody SejourRequest.Destination d) { service.sauverDestination(null,d); }
    /** Modifie une destination. */
    @PutMapping("/destinations/{id}")
    public void modifierDestination(@PathVariable Long id,@Valid @RequestBody SejourRequest.Destination d) { service.sauverDestination(id,d); }
    /** Supprime une destination inutilisee. */
    @DeleteMapping("/destinations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerDestination(@PathVariable Long id) { service.supprimerDestination(id); }
}
