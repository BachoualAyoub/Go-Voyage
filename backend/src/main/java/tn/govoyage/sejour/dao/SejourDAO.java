package tn.govoyage.sejour.dao;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SejourDAO {
    private final JdbcTemplate jdbc;

    public SejourDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> lire(String sql, Object... args) {
        return jdbc.queryForList(sql, args);
    }

    public int ecrire(String sql, Object... args) { return jdbc.update(sql, args); }

    public long compter(String sql, Object... args) {
        Long valeur = jdbc.queryForObject(sql, Long.class, args);
        return valeur == null ? 0 : valeur;
    }
}
