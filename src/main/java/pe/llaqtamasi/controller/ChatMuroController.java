package pe.llaqtamasi.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;

@RestController
@RequestMapping("/api/muro/chats")
public class ChatMuroController {
    private final Db db;
    private final Access access;

    public ChatMuroController(Db db, Access access) {
        this.db = db;
        this.access = access;
    }

    public record Mensaje(@NotNull UUID id, @NotBlank @Size(max = 2000) String texto) {}

    @GetMapping
    public List<Map<String, Object>> listar(@AuthenticationPrincipal Jwt jwt) {
        String usuario = access.user(jwt);
        return db.list("""
            SELECT c.publicacion_id, c.interesado_id, p.categoria,
                   u.alias, u.verificado
            FROM chat_muro c
            JOIN publicacion_muro p ON p.id = c.publicacion_id
            JOIN usuario u ON u.id = IF(p.autor_id = ?, c.interesado_id, p.autor_id)
            WHERE p.autor_id = ? OR c.interesado_id = ?
            ORDER BY p.categoria, c.publicacion_id, c.interesado_id
            """, usuario, usuario, usuario);
    }

    @GetMapping("/{publicacion}/{interesado}/detalle")
    public Map<String, Object> detalle(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicacion, @PathVariable UUID interesado) {
        String usuario = access.user(jwt);
        String contacto = interesado.toString();
        var aviso = db.one("""
            SELECT p.autor_id, p.tipo, p.categoria, p.texto, u.alias, u.verificado,
                   i.alias AS interesado_alias, i.verificado AS interesado_verificado
            FROM publicacion_muro p JOIN usuario u ON u.id = p.autor_id
            JOIN usuario i ON i.id = ?
            WHERE p.id = ?
            """, contacto, publicacion.toString());
        db.allow(usuario.equals(contacto) || (usuario.equals(aviso.get("autor_id"))
            && db.exists("SELECT 1 FROM chat_muro WHERE publicacion_id=? AND interesado_id=?",
                publicacion.toString(), contacto)));
        return aviso;
    }

    @GetMapping("/{publicacion}/{interesado}")
    public List<Map<String, Object>> mensajes(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicacion, @PathVariable UUID interesado) {
        String usuario = access.user(jwt);
        String aviso = publicacion.toString();
        String contacto = interesado.toString();
        String autor = (String) db.one("SELECT autor_id FROM publicacion_muro WHERE id=?", aviso).get("autor_id");
        db.allow(usuario.equals(autor) || usuario.equals(contacto));
        return db.list("""
            SELECT id, autor_id, texto FROM mensaje_muro
            WHERE publicacion_id=? AND interesado_id=? ORDER BY orden
            """, aviso, contacto);
    }

    @PostMapping("/{publicacion}/{interesado}")
    @Transactional
    public Map<String, String> enviar(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicacion, @PathVariable UUID interesado, @Valid @RequestBody Mensaje mensaje) {
        String usuario = access.user(jwt);
        String aviso = publicacion.toString();
        String contacto = interesado.toString();
        var publicacionMuro = db.one("""
            SELECT autor_id, IF(estado='ABIERTA' AND (expira_en IS NULL OR expira_en>UTC_TIMESTAMP(6)),
                'ABIERTA', 'CERRADA') estado
            FROM publicacion_muro WHERE id=? FOR UPDATE
            """, aviso);
        String autor = (String) publicacionMuro.get("autor_id");
        db.allow(!autor.equals(contacto) && (usuario.equals(autor) || usuario.equals(contacto)));
        if (!db.exists("SELECT 1 FROM chat_muro WHERE publicacion_id=? AND interesado_id=?", aviso, contacto)) {
            db.allow(usuario.equals(contacto) && "ABIERTA".equals(publicacionMuro.get("estado")));
            db.update("INSERT INTO chat_muro(publicacion_id, interesado_id) VALUES(?,?)", aviso, contacto);
        }
        String id = mensaje.id().toString();
        if (!db.exists("""
            SELECT 1 FROM mensaje_muro
            WHERE id=? AND publicacion_id=? AND interesado_id=? AND autor_id=? AND texto=?
            """, id, aviso, contacto, usuario, mensaje.texto().trim())) {
            db.update("INSERT INTO mensaje_muro(id,publicacion_id,interesado_id,autor_id,texto) VALUES(?,?,?,?,?)",
                id, aviso, contacto, usuario, mensaje.texto().trim());
        }
        return Map.of("id", id);
    }
}
