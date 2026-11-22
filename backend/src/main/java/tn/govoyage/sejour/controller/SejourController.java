package tn.govoyage.sejour.controller;

import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import tn.govoyage.security.CompteAuthentifie;
import tn.govoyage.sejour.dto.SejourRequest;
import tn.govoyage.sejour.service.SejourService;

/** Routes communes au web et a Android pour preparer et reserver un sejour. */
@RestController
@RequestMapping("/api")
public class SejourController {
    private final SejourService sejours;
    /** Recoit la logique de reservation. */
    public SejourController(SejourService sejours) { this.sejours = sejours; }
    /** Liste les hotels avec une estimation pour les dates demandees. */
    @GetMapping("/hotels")
    public List<Map<String,Object>> hotels(@RequestParam LocalDate arrivee, @RequestParam LocalDate depart,
            @RequestParam(defaultValue="2") int voyageurs, @RequestParam(defaultValue="1") int chambres) {
        return sejours.hotels(arrivee,depart,voyageurs,chambres);
    }
    /** Compare les offres de vol d'une etape. */
    @GetMapping("/vols")
    public List<Map<String,Object>> vols(@RequestParam String depart,@RequestParam String arrivee,@RequestParam LocalDate date) {
        return sejours.vols(depart,arrivee,date);
    }
    /** Retourne les reservations du compte connecte. */
    @GetMapping("/reservations")
    public List<Map<String,Object>> reservations(@AuthenticationPrincipal CompteAuthentifie c) { return sejours.reservations(c.id()); }
    /** Cree une reservation idempotente. */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> reserver(@AuthenticationPrincipal CompteAuthentifie c,@Valid @RequestBody SejourRequest.Reservation d) {
        return sejours.reserver(c.id(),d);
    }
    /** Annule une reservation du compte courant. */
    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void annuler(@AuthenticationPrincipal CompteAuthentifie c,@PathVariable Long id) { sejours.annuler(c.id(),id); }
}
