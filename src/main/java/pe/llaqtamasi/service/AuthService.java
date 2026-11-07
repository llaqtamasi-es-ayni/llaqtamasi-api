package pe.llaqtamasi.service;
import pe.llaqtamasi.entity.Usuario;
import pe.llaqtamasi.repository.Usuarios;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;
import jakarta.validation.Valid;
import static pe.llaqtamasi.dto.Requests.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@org.springframework.stereotype.Service
public class AuthService {
    private final Usuarios users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final Access access;
    private final Db db;
    public AuthService(Usuarios users, PasswordEncoder passwords, JwtEncoder encoder, Access access, Db db) {
        this.users=users; this.passwords=passwords; this.encoder=encoder; this.access=access; this.db=db;
    }
    @Transactional
    public Map<String,Object> register(Registro request) {
        db.require(request.password().getBytes(StandardCharsets.UTF_8).length<=72, "La clave supera 72 bytes");
        String login=request.nombreAcceso().toLowerCase(Locale.ROOT);
        if (users.findByNombreAcceso(login).isPresent()) throw new ResponseStatusException(CONFLICT,"Nombre de acceso ocupado");
        Usuario user=new Usuario();
        user.id=UUID.randomUUID().toString(); user.alias=request.alias().trim(); user.nombreAcceso=login;
        user.passwordHash=passwords.encode(request.password()); user.activo=true;
        user.registradoEn=LocalDateTime.now(ZoneOffset.UTC);
        users.saveAndFlush(user);
        return token(user);
    }
    public Map<String,Object> login(Login request) {
        Usuario user=users.findByNombreAcceso(request.nombreAcceso().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED,"Credenciales incorrectas"));
        if (!user.activo || user.passwordHash==null || !passwords.matches(request.password(),user.passwordHash))
            throw new ResponseStatusException(UNAUTHORIZED,"Credenciales incorrectas");
        return token(user);
    }
    private Map<String,Object> token(Usuario user) {
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("llaqtamasi").subject(user.id)
            .issuedAt(now).expiresAt(now.plusSeconds(3600)).build();
        String token=encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return Map.of("accessToken",token,"tokenType","Bearer","expiresIn",3600,"usuarioId",user.id);
    }
    public Map<String,Object> me(Jwt jwt) {
        String id=access.user(jwt);
        var result=db.one("SELECT id, alias, nombre_acceso, activo, verificado, nivel_confianza, registrado_en FROM usuario WHERE id=?",id);
        result.put("rolesComunitarios",db.list("SELECT r.id,r.nombre FROM rol_comunitario r JOIN usuario_rol_comunitario u ON r.id=u.rol_id WHERE u.usuario_id=? AND r.activo=TRUE",id));
        result.put("rolesSistema",db.list("SELECT r.id,r.nombre FROM rol_sistema r JOIN usuario_rol_sistema u ON r.id=u.rol_id WHERE u.usuario_id=? AND r.activo=TRUE",id));
        return result;
    }
    public Map<String,String> health() { return Map.of("estado","OK"); }
}
