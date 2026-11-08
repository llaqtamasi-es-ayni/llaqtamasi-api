package pe.llaqtamasi.service;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import static pe.llaqtamasi.dto.Requests.*;

@Service
public class ConsultaService {
    private final Access access;
    private final Db db;
    private final SolicitudService requests;

    public ConsultaService(Access access, Db db, SolicitudService requests) {
        this.access = access;
        this.db = db;
        this.requests = requests;
    }
    public List<Map<String,Object>> profiles(Jwt jwt,double lat,double lng,
        TipoPerfil tipo) {
        access.user(jwt);
        db.require(Double.isFinite(lat)&&Double.isFinite(lng)&&lat>=-90&&lat<=90&&lng>=-180&&lng<=180,"Coordenadas invalidas");
        return profilesInArea(CommunityService.point(lng,lat),tipo);
    }
    public List<Map<String,Object>> profilesInZone(Jwt jwt,UUID zonaId) {
        access.user(jwt);
        String area=(String)db.one("SELECT ST_AsText(limite,'axis-order=long-lat') area FROM zona WHERE id=?",zonaId.toString()).get("area");
        return profilesInArea(area,null);
    }
    private List<Map<String,Object>> profilesInArea(String area,TipoPerfil tipo) {
        return db.list("""
            SELECT p.id,p.usuario_id,u.alias,p.categoria_id,p.tipo,p.descripcion,p.nombre_establecimiento,
            (30.0+COALESCE(v.suma,0))/(10.0+COALESCE(v.total,0)) confianza,COALESCE(v.total,0) votos
            FROM perfil p JOIN usuario u ON u.id=p.usuario_id
            LEFT JOIN (SELECT perfil_id,SUM(valor) suma,COUNT(*) total FROM voto_perfil GROUP BY perfil_id) v ON v.perfil_id=p.id
            WHERE p.visible=TRUE AND u.activo=TRUE
            AND ST_Intersects(p.area_atencion,ST_GeomFromText(?,4326,'axis-order=long-lat'))
            AND (? IS NULL OR p.tipo=?) ORDER BY confianza DESC,p.id LIMIT 100
            """,area,tipo==null?null:tipo.name(),tipo==null?null:tipo.name());
    }
    public List<Map<String,Object>> requests(Jwt jwt) {
        String user=access.user(jwt);
        return db.list("""
            SELECT s.id,s.vecino_id,s.perfil_id,s.mensaje_inicial,s.estado,s.creada_en,s.finalizada_en,
                   v.id IS NOT NULL perfil_calificado, c.solicitud_id IS NOT NULL vecino_calificado
            FROM solicitud s JOIN perfil p ON p.id=s.perfil_id
            LEFT JOIN voto_perfil v ON v.solicitud_id=s.id
            LEFT JOIN calificacion_vecino c ON c.solicitud_id=s.id
            WHERE s.vecino_id=? OR p.usuario_id=? ORDER BY s.creada_en DESC LIMIT 100
            """,user,user);
    }
    public List<Map<String,Object>> messages(Jwt jwt,UUID id,
        int offset) {
        requests.participant(access.user(jwt),id.toString(),false);
        db.require(offset>=0,"Offset invalido");
        return db.list("SELECT id,autor_id,texto,enviado_en FROM mensaje WHERE solicitud_id=? ORDER BY enviado_en,id LIMIT 100 OFFSET ?",id.toString(),offset);
    }
    public List<Map<String,Object>> wall(Jwt jwt,UUID zonaId) {
        access.user(jwt);
        return db.list("""
            SELECT id,autor_id,zona_id,tipo,categoria,texto,estado,expira_en
            FROM publicacion_muro
            WHERE zona_id=? AND estado='ABIERTA' AND (expira_en IS NULL OR expira_en>UTC_TIMESTAMP(6))
            ORDER BY id LIMIT 100
            """,zonaId.toString());
    }
    public List<Map<String,Object>> reports(Jwt jwt,UUID zonaId) {
        access.user(jwt);
        return db.list("""
            SELECT r.id,r.autor_id,r.tipo,r.texto,r.direccion,ST_Latitude(r.ubicacion) lat,ST_Longitude(r.ubicacion) lng,r.creado_en,r.expira_en,
            (SELECT COUNT(*) FROM respuesta_reporte c WHERE c.reporte_id=r.id AND c.decision='CONFIRMA') confirmaciones,
            (SELECT COUNT(*) FROM respuesta_reporte c WHERE c.reporte_id=r.id AND c.decision='DESMIENTE') desmentidos
            FROM reporte_publico r JOIN zona z ON z.id=? AND (
                ST_Intersects(z.limite,r.ubicacion) OR (r.ubicacion IS NULL AND r.zona_id=z.id))
            WHERE r.expira_en>UTC_TIMESTAMP(6) ORDER BY r.creado_en DESC LIMIT 100
            """,zonaId.toString());
    }
}
