package tn.govoyage.destination.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.govoyage.destination.dto.DestinationResponse;
import tn.govoyage.destination.service.DestinationService;

/** Routes de consultation du catalogue. */
@RestController
@RequestMapping("/api/destinations")
@SecurityRequirement(name = "jeton")
public class DestinationController {
    private final DestinationService destinations;

    /** Recoit le service du catalogue. */
    public DestinationController(DestinationService destinations) {
        this.destinations = destinations;
    }

    /** Liste le catalogue. */
    @GetMapping
    public List<DestinationResponse> lister() {
        return destinations.lister();
    }

    /** Consulte une fiche. */
    @GetMapping("/{id}")
    public DestinationResponse consulter(@PathVariable Long id) {
        return destinations.consulter(id);
    }
}
