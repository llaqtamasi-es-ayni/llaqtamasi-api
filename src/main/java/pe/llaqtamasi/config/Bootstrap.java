package pe.llaqtamasi.config;
import pe.llaqtamasi.repository.Db;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Bootstrap implements ApplicationRunner {
    private final Db db;
    private final PasswordEncoder passwords;
    private final String login;
    private final String password;
    public Bootstrap(Db db,PasswordEncoder passwords,@Value("${app.bootstrap.login}") String login,
        @Value("${app.bootstrap.password}") String password) {
        this.db=db; this.passwords=passwords; this.login=login; this.password=password;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        String adminRole="00000000-0000-0000-0000-000000000001";
        db.update("INSERT INTO rol_sistema(id,nombre,descripcion,activo) VALUES(?,'ADMIN','Administracion',TRUE) ON DUPLICATE KEY UPDATE nombre=nombre",adminRole);
        if(login.isBlank()) return;
        db.require(login.matches("[a-zA-Z0-9_.-]{3,100}"),"ADMIN_LOGIN invalido");
        db.require(password.length()>=12 && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,"ADMIN_PASSWORD requiere 12 caracteres y maximo 72 bytes");
        // Only create a new account. Never promote an existing registration by matching its login.
        if(db.exists("SELECT id FROM usuario WHERE nombre_acceso=?",login)) return;
        String id=UUID.randomUUID().toString();
        db.update("""
            INSERT INTO usuario(id,alias,nombre_acceso,password_hash,activo,verificado,nivel_confianza,registrado_en)
            VALUES(?,'Administracion',?,?,TRUE,TRUE,0,UTC_TIMESTAMP(6))
            """,id,login.toLowerCase(Locale.ROOT),passwords.encode(password));
        String role=(String)db.one("SELECT id FROM rol_sistema WHERE nombre='ADMIN'").get("id");
        db.update("INSERT INTO usuario_rol_sistema(usuario_id,rol_id,asignado_en) VALUES(?,?,UTC_TIMESTAMP(6))",id,role);
    }
}
