package tn.govoyage.destination.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.govoyage.destination.dto.DestinationResponse;
import tn.govoyage.destination.service.DestinationService;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {
    private final DestinationService destinations;

    public DestinationController(DestinationService destinations) {
        this.destinations = destinations;
    }

    @GetMapping
    public List<DestinationResponse> lister() {
        return destinations.lister();
    }

    @GetMapping("/{id}")
    public DestinationResponse consulter(@PathVariable Long id) {
        return destinations.consulter(id);
    }
}
