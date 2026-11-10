package tn.govoyage.destination.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.govoyage.destination.dao.DestinationDAO;
import tn.govoyage.destination.dto.DestinationResponse;
import tn.govoyage.destination.entity.Destination;
import tn.govoyage.exception.RessourceIntrouvableException;

/** Prepare les informations du catalogue pour les clients. */
@Service
@Transactional(readOnly = true)
public class DestinationService {
    private final DestinationDAO destinations;

    /** Recoit le catalogue. */
    public DestinationService(DestinationDAO destinations) {
        this.destinations = destinations;
    }

    /** Liste les destinations disponibles. */
    public List<DestinationResponse> lister() {
        List<DestinationResponse> resultat = new ArrayList<>();
        for (Destination destination : destinations.findAllByOrderByNomAsc()) {
            resultat.add(versReponse(destination));
        }
        return resultat;
    }

    /** Consulte une destination. */
    public DestinationResponse consulter(Long id) {
        Destination destination = destinations.findById(id).orElse(null);
        if (destination == null) throw new RessourceIntrouvableException("Destination introuvable.");
        return versReponse(destination);
    }

    /** Transforme une destination en donnees JSON. */
    private DestinationResponse versReponse(Destination destination) {
        return new DestinationResponse(destination.getId(), destination.getCode(),
                destination.getNom(), destination.getVille(), destination.getPays(),
                destination.getRegion(), destination.getAmbiance(), destination.getTarifJourTnd(),
                destination.getAccroche(), destination.getDescription());
    }
}
