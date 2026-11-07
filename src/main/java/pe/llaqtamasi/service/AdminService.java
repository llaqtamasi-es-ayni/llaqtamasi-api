package pe.llaqtamasi.service;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import static pe.llaqtamasi.dto.Requests.*;

@Transactional
@org.springframework.stereotype.Service
public class AdminService {
    private final Db db;
    private final Access access;
    public AdminService(Db db,Access access) { this.db=db; this.access=access; }
    private String table(Catalog type) {
        return switch(type) {
            case categorias -> "categoria_trabajo";
            case rolesComunitarios -> "rol_comunitario";
            case rolesSistema -> "rol_sistema";
        };
    }
    public List<Map<String,Object>> catalog(Jwt jwt,Catalog tipo) {
        access.user(jwt);
        return db.list("SELECT * FROM "+table(tipo)+" ORDER BY nombre LIMIT 500");
    }
    public Map<String,String> catalog(Jwt jwt,Catalog tipo,Catalogo c) {
        access.admin(access.user(jwt));
        if(tipo==Catalog.rolesSistema) db.require(c.nombre().length()<=50,"El rol admite 50 caracteres");
        String id=UUID.randomUUID().toString();
        String active=tipo==Catalog.categorias?"activa":"activo";
        db.update("INSERT INTO "+table(tipo)+"(id,nombre,descripcion,"+active+") VALUES(?,?,?,TRUE)",id,c.nombre(),c.descripcion());
        return Map.of("id",id);
    }
    public void assign(Jwt jwt,UUID usuarioId,Catalog tipo,UUID rolId) {
        access.admin(access.user(jwt));
        db.require(tipo!=Catalog.categorias,"Selecciona un tipo de rol");
        String target=tipo==Catalog.rolesSistema?"usuario_rol_sistema":"usuario_rol_comunitario";
        db.update("INSERT INTO "+target+"(usuario_id,rol_id,asignado_en) VALUES(?,?,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE asignado_en=asignado_en",usuarioId.toString(),rolId.toString());
    }
    public void revoke(Jwt jwt,UUID usuarioId,Catalog tipo,UUID rolId) {
        String current=access.user(jwt); access.admin(current);
        db.require(!current.equals(usuarioId.toString()),"Otro administrador debe retirar tu rol");
        db.require(tipo!=Catalog.categorias,"Selecciona un tipo de rol");
        String target=tipo==Catalog.rolesSistema?"usuario_rol_sistema":"usuario_rol_comunitario";
        db.update("DELETE FROM "+target+" WHERE usuario_id=? AND rol_id=?",usuarioId.toString(),rolId.toString());
    }
    public void verify(Jwt jwt,UUID id) {
        access.admin(access.user(jwt));
        db.userLock(id.toString());
        db.update("UPDATE usuario SET verificado=TRUE WHERE id=?",id.toString());
    }
    public List<Map<String,Object>> zones(Jwt jwt) {
        access.user(jwt);
        return db.list("SELECT id,nombre,ST_AsText(limite,'axis-order=long-lat') limite_wkt FROM zona ORDER BY nombre LIMIT 500");
    }
    public void zone(Jwt jwt,Zona z) {
        access.admin(access.user(jwt));
        db.require(z.limiteWkt().trim().toUpperCase(Locale.ROOT).startsWith("MULTIPOLYGON"),"Se requiere MULTIPOLYGON");
        db.update("INSERT INTO zona(id,nombre,limite) VALUES(?,?,ST_GeomFromText(?,4326,'axis-order=long-lat'))",z.id().toString(),z.nombre(),z.limiteWkt());
    }
    public List<Map<String,Object>> centers(Jwt jwt,UUID zonaId) {
        access.user(jwt);
        return centersIn(zonaId.toString());
    }
    private List<Map<String,Object>> centersIn(String zone) {
        return db.list("SELECT id,zona_id,nombre,tipo,ST_Latitude(ubicacion) lat,ST_Longitude(ubicacion) lng,horario,contacto_publico FROM centro WHERE zona_id=? ORDER BY nombre LIMIT 500",zone);
    }
    public void center(Jwt jwt,Centro c) {
        access.admin(access.user(jwt));
        db.require(Double.isFinite(c.lat())&&Double.isFinite(c.lng()),"Coordenadas invalidas");
        db.update("""
            INSERT INTO centro(id,zona_id,nombre,tipo,ubicacion,horario,contacto_publico)
            VALUES(?,?,?,?,ST_GeomFromText(?,4326,'axis-order=long-lat'),?,?)
            """,c.id().toString(),c.zonaId().toString(),c.nombre(),c.tipo().name(),CommunityService.point(c.lng(),c.lat()),c.horario(),c.contactoPublico());
    }
    public void packageMap(Jwt jwt,Paquete p) {
        access.admin(access.user(jwt));
        db.require(p.referenciaArchivo().startsWith("https://"),"El paquete requiere una URL HTTPS");
        db.update("INSERT INTO paquete_mapa(id,zona_id,version,referencia_archivo,generado_en) VALUES(?,?,?,?,UTC_TIMESTAMP(6))",
            p.id().toString(),p.zonaId().toString(),p.version(),p.referenciaArchivo());
    }
    public Map<String,Object> packageMap(Jwt jwt,UUID id) {
        access.user(jwt);
        return Map.of("paquete",db.one("SELECT id,zona_id,version,referencia_archivo,generado_en FROM paquete_mapa WHERE zona_id=? ORDER BY version DESC LIMIT 1",id.toString()),"centros",centersIn(id.toString()));
    }
}
