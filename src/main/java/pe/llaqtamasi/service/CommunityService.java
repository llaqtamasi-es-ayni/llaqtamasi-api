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
        if (p.tipo()!=TipoPerfil.TIENDA) access.verified(user);
        db.one("SELECT id FROM categoria_trabajo WHERE id=? AND activa=TRUE",p.categoriaId().toString());
        db.require(p.areaWkt().trim().toUpperCase(Locale.ROOT).startsWith("MULTIPOLYGON"),"Se requiere MULTIPOLYGON");
        db.update("""
            INSERT INTO perfil(id,usuario_id,categoria_id,tipo,descripcion,area_atencion,visible)
            VALUES(?,?,?,?,?,ST_GeomFromText(?,4326,'axis-order=long-lat'),TRUE)
            """,p.id().toString(),user,p.categoriaId().toString(),p.tipo().name(),p.descripcion(),p.areaWkt());
        return p.id().toString();
    }
    public void visibility(String user, String id, boolean visible) {
        var row=db.one("SELECT usuario_id,tipo FROM perfil WHERE id=? FOR UPDATE",id);
        db.allow(user.equals(row.get("usuario_id")));
        if (visible && !"TIENDA".equals(row.get("tipo"))) access.verified(user);
        db.update("UPDATE perfil SET visible=? WHERE id=?",visible,id);
    }
    public void endorse(String user, String target) {
        db.require(!user.equals(target),"No puedes avalarte");
        // Lock the recipient to serialize concurrent endorsements and threshold checks.
        db.userLock(target);
        access.verified(user);
        db.update("INSERT INTO aval_vecinal(avalista_id,avalado_id,creado_en) VALUES(?,?,UTC_TIMESTAMP(6))",user,target);
        Number count=(Number)db.one("""
            SELECT COUNT(*) total FROM aval_vecinal a JOIN usuario u ON u.id=a.avalista_id
            WHERE a.avalado_id=? AND u.verificado=TRUE AND u.activo=TRUE
            """,target).get("total");
        if(count.intValue()>=required) db.update("UPDATE usuario SET verificado=TRUE WHERE id=?",target);
    }
    public void vote(String user, String profile, int value) {
        var row=db.one("SELECT usuario_id FROM perfil WHERE id=?",profile);
        db.require(!user.equals(row.get("usuario_id")),"No puedes votar tu perfil");
        db.allow(db.exists("SELECT id FROM solicitud WHERE vecino_id=? AND perfil_id=? AND estado='FINALIZADA'",user,profile));
        db.update("""
            INSERT INTO voto_perfil(autor_id,perfil_id,valor,creado_en) VALUES(?,?,?,UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE valor=?,creado_en=UTC_TIMESTAMP(6)
            """,user,profile,value,value);
    }
    public String wall(String user, Muro p) {
        db.update("""
            INSERT INTO publicacion_muro(id,autor_id,zona_id,tipo,categoria,texto,estado)
            VALUES(?,?,?,?,?,?,'ABIERTA')
            """,p.id().toString(),user,p.zonaId().toString(),p.tipo().name(),p.categoria(),p.texto());
        return p.id().toString();
    }
    public void closeWall(String user,String id) {
        var row=db.one("SELECT autor_id FROM publicacion_muro WHERE id=? FOR UPDATE",id);
        db.allow(user.equals(row.get("autor_id")));
        db.update("UPDATE publicacion_muro SET estado='CERRADA' WHERE id=?",id);
    }
    public String report(String user, Reporte r) {
        db.require(Double.isFinite(r.lat()) && Double.isFinite(r.lng()),"Coordenadas invalidas");
        db.update("""
            INSERT INTO reporte_publico(id,autor_id,tipo,ubicacion,texto,creado_en,expira_en)
            VALUES(?,?,?,ST_GeomFromText(?,4326,'axis-order=long-lat'),?,UTC_TIMESTAMP(6),
            TIMESTAMPADD(HOUR,?,UTC_TIMESTAMP(6)))
            """,r.id().toString(),user,r.tipo(),point(r.lng(),r.lat()),r.texto(),r.duracionHoras());
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
