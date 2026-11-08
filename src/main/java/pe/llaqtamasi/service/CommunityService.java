package pe.llaqtamasi.service;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static pe.llaqtamasi.dto.Requests.*;

@Service
@Transactional
public class CommunityService {
    private final Db db;
    private final Access access;
    private final int required;
    public CommunityService(Db db, Access access, @Value("${app.avales-requeridos}") int required) {
        this.db=db; this.access=access; this.required=required;
    }
    public String profile(String user, Perfil p) {
        access.verified(user);
        String establecimiento = p.nombreEstablecimiento() == null ? null : p.nombreEstablecimiento().trim();
        if (establecimiento != null) {
            db.require(p.tipo() == TipoPerfil.TIENDA && !establecimiento.isBlank(),
                "Indica un nombre válido para el establecimiento");
        }
        db.one("SELECT id FROM categoria_trabajo WHERE id=? AND activa=TRUE",p.categoriaId().toString());
        db.require(p.areaWkt().trim().toUpperCase(Locale.ROOT).startsWith("MULTIPOLYGON"),"Se requiere MULTIPOLYGON");
        db.update("""
            INSERT INTO perfil(id,usuario_id,categoria_id,tipo,descripcion,area_atencion,visible,nombre_establecimiento)
            VALUES(?,?,?,?,?,ST_GeomFromText(?,4326,'axis-order=long-lat'),TRUE,?)
            """,p.id().toString(),user,p.categoriaId().toString(),p.tipo().name(),p.descripcion(),p.areaWkt(),establecimiento);
        return p.id().toString();
    }
    public void visibility(String user, String id, boolean visible) {
        var row=db.one("SELECT usuario_id,tipo FROM perfil WHERE id=? FOR UPDATE",id);
        db.allow(user.equals(row.get("usuario_id")));
        if (visible && !"TIENDA".equals(row.get("tipo"))) access.verified(user);
        db.update("UPDATE perfil SET visible=? WHERE id=?",visible,id);
    }
    public List<Map<String,Object>> vecinosParaAval(String user, String consulta) {
        access.verified(user);
        String termino = consulta.trim();
        db.require(termino.length() >= 2 && termino.length() <= 150, "Escribe entre 2 y 150 caracteres");
        return db.list("""
            SELECT u.id,u.alias,u.nombre_acceso,
                   EXISTS(SELECT 1 FROM aval_vecinal a WHERE a.avalista_id=? AND a.avalado_id=u.id) ya_avalado
            FROM usuario u
            WHERE u.activo=TRUE AND u.verificado=FALSE AND u.id<>?
              AND (LOCATE(?,u.alias)>0 OR LOCATE(?,u.nombre_acceso)>0)
            ORDER BY u.alias,u.id LIMIT 20
            """,user,user,termino,termino);
    }

    public void endorse(String user, String target) {
        db.require(!user.equals(target),"No puedes avalarte");
        access.verified(user);
        // Lock the recipient to serialize concurrent endorsements and threshold checks.
        db.userLock(target);
        db.update("""
            INSERT INTO aval_vecinal(avalista_id,avalado_id,creado_en) VALUES(?,?,UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE creado_en=aval_vecinal.creado_en
            """,user,target);
        Number count=(Number)db.one("""
            SELECT COUNT(*) total FROM aval_vecinal a JOIN usuario u ON u.id=a.avalista_id
            WHERE a.avalado_id=? AND u.verificado=TRUE AND u.activo=TRUE
            """,target).get("total");
        if(count.intValue()>=required) db.update("UPDATE usuario SET verificado=TRUE WHERE id=?",target);
    }
    public void vote(String user, String profile, int value) {
        var solicitudes=db.list("""
            SELECT id FROM solicitud WHERE vecino_id=? AND perfil_id=? AND estado='FINALIZADA'
            ORDER BY finalizada_en DESC,id DESC LIMIT 1
            """,user,profile);
        db.allow(!solicitudes.isEmpty());
        voteRequest(user,(String)solicitudes.getFirst().get("id"),value);
    }

    public void voteRequest(String user, String solicitud, int value) {
        var row=db.one("""
            SELECT s.vecino_id,s.perfil_id,s.estado,p.usuario_id
            FROM solicitud s JOIN perfil p ON p.id=s.perfil_id WHERE s.id=?
            """,solicitud);
        String proveedor=(String)row.get("usuario_id");
        db.allow(user.equals(row.get("vecino_id")) && !user.equals(proveedor));
        db.require("FINALIZADA".equals(row.get("estado")),"La solicitud debe estar finalizada");
        db.userLock(proveedor);
        db.update("""
            INSERT INTO voto_perfil(autor_id,perfil_id,solicitud_id,valor,creado_en)
            VALUES(?,?,?,?,UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE valor=voto_perfil.valor
            """,user,row.get("perfil_id"),solicitud,value);
        db.verificarPorCalificaciones(proveedor);
    }
    public String wall(String user, Muro p) {
        db.update("""
            INSERT INTO publicacion_muro(id,autor_id,zona_id,tipo,categoria,texto,estado,expira_en)
            VALUES(?,?,?,?,?,?,'ABIERTA',TIMESTAMPADD(HOUR,?,UTC_TIMESTAMP(6)))
            """,p.id().toString(),user,p.zonaId().toString(),p.tipo().name(),p.categoria(),p.texto(),
            p.duracionHoras()==null?24:p.duracionHoras());
        return p.id().toString();
    }
    public void closeWall(String user,String id) {
        var row=db.one("SELECT autor_id FROM publicacion_muro WHERE id=? FOR UPDATE",id);
        db.allow(user.equals(row.get("autor_id")));
        db.update("UPDATE publicacion_muro SET estado='CERRADA' WHERE id=?",id);
    }
    public String report(String user, Reporte r) {
        access.verified(user);
        boolean tienePunto = r.lat() != null && r.lng() != null;
        db.require(tienePunto || (r.lat() == null && r.lng() == null),"Coordenadas incompletas");
        String direccion = r.direccion() == null ? "" : r.direccion().trim();
        String zona = r.zonaId() == null ? null : r.zonaId().toString();
        db.require(tienePunto || (!direccion.isBlank() && zona != null),"Indica un punto o una direccion y su zona");
        db.require(!tienePunto || (Double.isFinite(r.lat()) && Double.isFinite(r.lng())),"Coordenadas invalidas");
        String punto = tienePunto ? point(r.lng(),r.lat()) : null;
        if (zona != null) {
            db.require(db.exists("""
                SELECT id FROM zona WHERE id=? AND (? IS NULL OR
                    ST_Intersects(limite,ST_GeomFromText(?,4326,'axis-order=long-lat')))
                """,zona,punto,punto),"El lugar debe pertenecer a la zona seleccionada");
        }
        db.update("""
            INSERT INTO reporte_publico(id,autor_id,tipo,ubicacion,direccion,zona_id,texto,creado_en,expira_en)
            VALUES(?,?,?,ST_GeomFromText(?,4326,'axis-order=long-lat'),?,?,?,UTC_TIMESTAMP(6),
            TIMESTAMPADD(HOUR,?,UTC_TIMESTAMP(6)))
            """,r.id().toString(),user,r.tipo(),punto,direccion.isBlank()?null:direccion,zona,r.texto(),r.duracionHoras());
        return r.id().toString();
    }
    public void respond(String user, String id, Decision decision) {
        var report=db.one("SELECT autor_id FROM reporte_publico WHERE id=? AND expira_en>UTC_TIMESTAMP(6) FOR UPDATE",id);
        db.require(!user.equals(report.get("autor_id")),"El autor no confirma su propio reporte");
        db.update("""
            INSERT INTO respuesta_reporte(reporte_id,usuario_id,decision,actualizada_en)
            VALUES(?,?,?,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE decision=?,actualizada_en=UTC_TIMESTAMP(6)
            """,id,user,decision.name(),decision.name());
    }
    public static String point(double lng,double lat) { return "POINT("+lng+" "+lat+")"; }
}
