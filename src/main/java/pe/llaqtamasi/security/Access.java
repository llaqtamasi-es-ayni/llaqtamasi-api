package pe.llaqtamasi.security;
import pe.llaqtamasi.repository.Db;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Component
public class Access {
    private final Db db;
    public Access(Db db) { this.db = db; }
    public String user(Jwt jwt) {
        if (jwt == null || !db.exists("SELECT id FROM usuario WHERE id=? AND activo=TRUE", jwt.getSubject()))
            throw new ResponseStatusException(UNAUTHORIZED, "Cuenta inactiva");
        return jwt.getSubject();
    }
    public void admin(String id) {
        db.allow(db.exists("""
            SELECT u.usuario_id FROM usuario_rol_sistema u JOIN rol_sistema r ON r.id=u.rol_id
            WHERE u.usuario_id=? AND r.nombre='ADMIN' AND r.activo=TRUE
            """, id));
    }
    public void verified(String id) {
        db.allow(db.exists("SELECT id FROM usuario WHERE id=? AND verificado=TRUE AND activo=TRUE", id));
    }
}
