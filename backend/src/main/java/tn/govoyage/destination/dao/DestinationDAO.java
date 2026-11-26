package tn.govoyage.destination.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.govoyage.destination.entity.Destination;

public interface DestinationDAO extends JpaRepository<Destination, Long> {

    List<Destination> findAllByOrderByNomAsc();
}
