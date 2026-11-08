package tn.govoyage.utilisateur.dao;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tn.govoyage.utilisateur.entity.Utilisateur;

/** Accès à la table utilisateur (DAO). */
public interface UtilisateurDAO extends JpaRepository<Utilisateur, Long> {

    /** Charge un compte et bloque ses mises a jour concurrentes jusqu'a la fin de la transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.id = :id")
    Optional<Utilisateur> verrouillerParId(@Param("id") Long id);

    /** Recherche un utilisateur par son e-mail. */
    @Query("select u from Utilisateur u where u.email = :email")
    Optional<Utilisateur> chercherParEmail(@Param("email") String email);

    /** Indique si un e-mail est déjà présent en base. */
    @Query("select count(u) > 0 from Utilisateur u where u.email = :email")
    boolean emailExiste(@Param("email") String email);
}
