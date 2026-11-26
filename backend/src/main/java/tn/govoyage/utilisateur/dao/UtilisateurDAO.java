package tn.govoyage.utilisateur.dao;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.govoyage.utilisateur.entity.Utilisateur;

public interface UtilisateurDAO extends JpaRepository<Utilisateur, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.id = :id")
    Optional<Utilisateur> verrouillerParId(@Param("id") Long id);

    @Query("select u from Utilisateur u where u.email = :email")
    Optional<Utilisateur> chercherParEmail(@Param("email") String email);

    @Query("select count(u) > 0 from Utilisateur u where u.email = :email")
    boolean emailExiste(@Param("email") String email);
}
