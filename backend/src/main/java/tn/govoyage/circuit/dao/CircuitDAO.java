package tn.govoyage.circuit.dao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.govoyage.circuit.entity.Circuit;

/** Separe les modeles publics des circuits personnels. */
public interface CircuitDAO extends JpaRepository<Circuit, Long> {
    /** Charge les modeles proposes. */
    List<Circuit> findByProprietaireIdIsNullOrderByIdAsc();
    /** Charge les circuits d'un seul compte. */
    List<Circuit> findByProprietaireIdOrderByIdDesc(Long proprietaireId);
    /** Charge un circuit seulement s'il appartient au compte. */
    Optional<Circuit> findByIdAndProprietaireId(Long id, Long proprietaireId);
}
