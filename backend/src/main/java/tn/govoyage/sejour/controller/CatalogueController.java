package tn.govoyage.sejour.controller;

import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import tn.govoyage.sejour.dto.SejourRequest;
import tn.govoyage.sejour.service.CatalogueService;

@RestController
@RequestMapping("/api/admin")
public class CatalogueController {
    private final CatalogueService service;

    public CatalogueController(CatalogueService service) { this.service=service; }

    @GetMapping("/statistiques") public Map<String,Object> statistiques() { return service.statistiques(); }

    @GetMapping("/hotels") public List<Map<String,Object>> hotels() { return service.hotels(); }

    @PostMapping("/hotels") @ResponseStatus(HttpStatus.CREATED)
    public void creerHotel(@Valid @RequestBody SejourRequest.Hotel d) { service.sauverHotel(null,d); }

    @PutMapping("/hotels/{id}")
    public void modifierHotel(@PathVariable Long id,@Valid @RequestBody SejourRequest.Hotel d) { service.sauverHotel(id,d); }

    @DeleteMapping("/hotels/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirerHotel(@PathVariable Long id) { service.retirerHotel(id); }

    @PostMapping("/destinations") @ResponseStatus(HttpStatus.CREATED)
    public void creerDestination(@Valid @RequestBody SejourRequest.Destination d) { service.sauverDestination(null,d); }

    @PutMapping("/destinations/{id}")
    public void modifierDestination(@PathVariable Long id,@Valid @RequestBody SejourRequest.Destination d) { service.sauverDestination(id,d); }

    @DeleteMapping("/destinations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerDestination(@PathVariable Long id) { service.supprimerDestination(id); }
}
