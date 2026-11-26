package tn.govoyage.circuit.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.govoyage.circuit.dto.CircuitRequest;
import tn.govoyage.circuit.dto.CircuitResponse;
import tn.govoyage.circuit.service.CircuitService;
import tn.govoyage.security.CompteAuthentifie;

@RestController
@RequestMapping("/api/circuits")
public class CircuitController {
    private final CircuitService circuits;

    public CircuitController(CircuitService circuits) {
        this.circuits = circuits;
    }

    @GetMapping("/modeles")
    public List<CircuitResponse> modeles() { return circuits.listerModeles(); }

    @SecurityRequirement(name = "jeton")
    @GetMapping
    public List<CircuitResponse> lister(@AuthenticationPrincipal CompteAuthentifie compte) {
        return circuits.lister(compte.id());
    }

    @SecurityRequirement(name = "jeton")
    @GetMapping("/{id}")
    public CircuitResponse consulter(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie compte) {
        return circuits.consulter(id, compte.id());
    }

    @SecurityRequirement(name = "jeton")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CircuitResponse creer(@AuthenticationPrincipal CompteAuthentifie compte,
                                 @Valid @RequestBody CircuitRequest demande) {
        return circuits.creer(compte.id(), demande);
    }

    @SecurityRequirement(name = "jeton")
    @PutMapping("/{id}")
    public CircuitResponse modifier(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie compte,
                                    @Valid @RequestBody CircuitRequest demande) {
        return circuits.modifier(id, compte.id(), demande);
    }

    @SecurityRequirement(name = "jeton")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie compte) {
        circuits.supprimer(id, compte.id());
    }
}
