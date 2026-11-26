package tn.govoyage.circuit.dao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.govoyage.circuit.entity.Circuit;

public interface CircuitDAO extends JpaRepository<Circuit, Long> {

    List<Circuit> findByProprietaireIdIsNullOrderByIdAsc();

    List<Circuit> findByProprietaireIdOrderByIdDesc(Long proprietaireId);

    Optional<Circuit> findByIdAndProprietaireId(Long id, Long proprietaireId);
}
