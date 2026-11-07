package pe.llaqtamasi.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Repository
public class Db {
    final JdbcTemplate jdbc;
    public Db(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Map<String, Object>> list(String sql, Object... args) { return jdbc.queryForList(sql, args); }
    public Map<String, Object> one(String sql, Object... args) {
        var rows = list(sql, args);
        if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "Registro no encontrado");
        return rows.getFirst();
    }
    public int update(String sql, Object... args) { return jdbc.update(sql, args); }
    public boolean exists(String sql, Object... args) { return !list(sql, args).isEmpty(); }
    public void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(BAD_REQUEST, message);
    }
    public void allow(boolean condition) {
        if (!condition) throw new ResponseStatusException(FORBIDDEN, "Acceso denegado");
    }
    public void userLock(String id) { one("SELECT id FROM usuario WHERE id=? AND activo=TRUE FOR UPDATE", id); }
}
