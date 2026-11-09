package tn.govoyage.destination.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.govoyage.destination.entity.Destination;

/** Lit les destinations de reference. */
public interface DestinationDAO extends JpaRepository<Destination, Long> {
    /** Classe le catalogue par nom. */
    List<Destination> findAllByOrderByNomAsc();
}
