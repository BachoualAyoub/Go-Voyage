package tn.govoyage.circuit.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.govoyage.circuit.dao.CircuitDAO;
import tn.govoyage.circuit.dto.CircuitRequest;
import tn.govoyage.circuit.dto.CircuitResponse;
import tn.govoyage.circuit.dto.EtapeCircuitRequest;
import tn.govoyage.circuit.dto.EtapeCircuitResponse;
import tn.govoyage.circuit.entity.Circuit;
import tn.govoyage.circuit.entity.EtapeCircuit;
import tn.govoyage.destination.dao.DestinationDAO;
import tn.govoyage.destination.entity.Destination;
import tn.govoyage.exception.RessourceIntrouvableException;

/** Gere les plans personnels et calcule les estimations du catalogue. */
@Service
@Transactional(readOnly = true)
public class CircuitService {
    private final CircuitDAO circuits;
    private final DestinationDAO destinations;

    /** Recoit les repositories JPA du parcours. */
    public CircuitService(CircuitDAO circuits, DestinationDAO destinations) {
        this.circuits = circuits;
        this.destinations = destinations;
    }

    /** Liste les modeles consultables. */
    public List<CircuitResponse> listerModeles() {
        return versListe(circuits.findByProprietaireIdIsNullOrderByIdAsc());
    }

    /** Liste les circuits du compte courant. */
    public List<CircuitResponse> lister(Long compteId) {
        return versListe(circuits.findByProprietaireIdOrderByIdDesc(compteId));
    }

    /** Consulte un modele ou un circuit appartenant au compte. */
    public CircuitResponse consulter(Long id, Long compteId) {
        Circuit circuit = circuits.findById(id).orElse(null);
        if (circuit == null || (circuit.getProprietaireId() != null
                && !circuit.getProprietaireId().equals(compteId))) {
            throw new RessourceIntrouvableException("Circuit introuvable.");
        }
        return versReponse(circuit);
    }

    /** Enregistre un plan au nom du compte courant. */
    @Transactional
    public CircuitResponse creer(Long compteId, CircuitRequest demande) {
        return enregistrer(new Circuit(compteId), demande);
    }

    /** Actualise la revision d'un circuit personnel. */
    @Transactional
    public CircuitResponse modifier(Long id, Long compteId, CircuitRequest demande) {
        Circuit circuit = trouverPersonnel(id, compteId);
        if (demande.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La version du circuit est requise.");
        }
        if (!circuit.getVersion().equals(demande.version())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce circuit a change. Rechargez la liste.");
        }
        return enregistrer(circuit, demande);
    }

    /** Supprime uniquement un circuit du compte courant. */
    @Transactional
    public void supprimer(Long id, Long compteId) {
        circuits.delete(trouverPersonnel(id, compteId));
    }

    /** Evite de reveler les circuits des autres voyageurs. */
    private Circuit trouverPersonnel(Long id, Long compteId) {
        Circuit circuit = circuits.findByIdAndProprietaireId(id, compteId).orElse(null);
        if (circuit == null) throw new RessourceIntrouvableException("Circuit introuvable.");
        return circuit;
    }

    /** Valide les references et enregistre les etapes dans l'ordre. */
    private CircuitResponse enregistrer(Circuit circuit, CircuitRequest demande) {
        int duree = 0;
        List<EtapeCircuit> etapes = new ArrayList<>();
        for (EtapeCircuitRequest etape : demande.etapes()) {
            Destination destination = destinations.findById(etape.destinationId()).orElse(null);
            if (destination == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une destination est introuvable.");
            }
            duree += etape.jours();
            etapes.add(new EtapeCircuit(destination, etape.jours()));
        }
        if (duree > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un circuit dure au maximum 60 jours.");
        }
        circuit.actualiser(demande.nom().trim(), demande.budgetTnd(), etapes);
        return versReponse(circuits.saveAndFlush(circuit));
    }

    /** Convertit les circuits en reponses. */
    private List<CircuitResponse> versListe(List<Circuit> liste) {
        List<CircuitResponse> resultat = new ArrayList<>();
        for (Circuit circuit : liste) resultat.add(versReponse(circuit));
        return resultat;
    }

    /** Calcule les totaux a partir des seuls tarifs du catalogue. */
    private CircuitResponse versReponse(Circuit circuit) {
        List<EtapeCircuitResponse> etapes = new ArrayList<>();
        int duree = 0;
        int estimation = 0;
        for (EtapeCircuit etape : circuit.getEtapes()) {
            Destination destination = etape.getDestination();
            int cout = etape.getJours() * destination.getTarifJourTnd();
            duree += etape.getJours();
            estimation += cout;
            etapes.add(new EtapeCircuitResponse(destination.getId(), destination.getNom(),
                    destination.getVille(), destination.getAmbiance(), etape.getJours(),
                    destination.getTarifJourTnd(), cout));
        }
        return new CircuitResponse(circuit.getId(), circuit.getNom(), circuit.getDescription(),
                circuit.getProprietaireId() == null, circuit.getBudgetTnd(), duree, estimation,
                circuit.getVersion(), etapes);
    }
}
