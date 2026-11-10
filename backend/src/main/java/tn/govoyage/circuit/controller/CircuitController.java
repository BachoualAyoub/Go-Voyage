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

/** Routes de consultation et de personnalisation des circuits. */
@RestController
@RequestMapping("/api/circuits")
public class CircuitController {
    private final CircuitService circuits;

    /** Recoit la logique de planification. */
    public CircuitController(CircuitService circuits) {
        this.circuits = circuits;
    }

    /** Liste les circuits proposes, ouverte a tous. */
    @GetMapping("/modeles")
    public List<CircuitResponse> modeles() { return circuits.listerModeles(); }

    /** Liste les circuits du voyageur connecte. */
    @SecurityRequirement(name = "jeton")
    @GetMapping
    public List<CircuitResponse> lister(@AuthenticationPrincipal CompteAuthentifie compte) {
        return circuits.lister(compte.id());
    }

    /** Consulte un circuit accessible. */
    @SecurityRequirement(name = "jeton")
    @GetMapping("/{id}")
    public CircuitResponse consulter(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie compte) {
        return circuits.consulter(id, compte.id());
    }

    /** Enregistre un circuit personnel. */
    @SecurityRequirement(name = "jeton")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CircuitResponse creer(@AuthenticationPrincipal CompteAuthentifie compte,
                                 @Valid @RequestBody CircuitRequest demande) {
        return circuits.creer(compte.id(), demande);
    }

    /** Remplace les etapes et le budget. */
    @SecurityRequirement(name = "jeton")
    @PutMapping("/{id}")
    public CircuitResponse modifier(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie compte,
                                    @Valid @RequestBody CircuitRequest demande) {
        return circuits.modifier(id, compte.id(), demande);
    }

    /** Supprime un circuit personnel. */
    @SecurityRequirement(name = "jeton")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id, @AuthenticationPrincipal CompteAuthentifie compte) {
        circuits.supprimer(id, compte.id());
    }
}
