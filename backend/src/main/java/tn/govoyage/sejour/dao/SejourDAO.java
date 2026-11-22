package tn.govoyage.sejour.dao;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Requetes parametrees des catalogues, reservations et statistiques. */
@Repository
public class SejourDAO {
    private final JdbcTemplate jdbc;
    /** Recoit le client JDBC partage par Spring. */
    public SejourDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    /** Lit les lignes d'une requete interne parametree. */
    public List<Map<String,Object>> lire(String sql, Object... args) {
        return jdbc.queryForList(sql, args);
    }
    /** Execute une ecriture dans la transaction du service. */
    public int ecrire(String sql, Object... args) { return jdbc.update(sql, args); }
    /** Lit un compteur ou un total. */
    public long compter(String sql, Object... args) {
        Long valeur = jdbc.queryForObject(sql, Long.class, args);
        return valeur == null ? 0 : valeur;
    }
}
