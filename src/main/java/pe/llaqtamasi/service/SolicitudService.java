package pe.llaqtamasi.service;
import pe.llaqtamasi.repository.Db;
import pe.llaqtamasi.security.Access;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static pe.llaqtamasi.dto.Requests.*;

@Service
@Transactional
public class SolicitudService {
    private final Db db;
    private final Access access;
    private final ApplicationEventPublisher events;
    public SolicitudService(Db db,Access access,ApplicationEventPublisher events) {
        this.db=db; this.access=access; this.events=events;
    }
    public String create(String user,Solicitud request) {
        var profile=db.one("SELECT p.usuario_id,p.tipo FROM perfil p JOIN usuario u ON u.id=p.usuario_id WHERE p.id=? AND p.visible=TRUE AND u.activo=TRUE",request.perfilId().toString());
        db.require(!user.equals(profile.get("usuario_id")),"No puedes solicitar tu propio perfil");
        if (!"TIENDA".equals(profile.get("tipo"))) access.verified(user);
        db.update("""
            INSERT INTO solicitud(id,vecino_id,perfil_id,mensaje_inicial,estado,creada_en)
            VALUES(?,?,?,?,'PENDIENTE',UTC_TIMESTAMP(6))
            """,request.id().toString(),user,request.perfilId().toString(),request.mensaje());
        return request.id().toString();
    }
    public Map<String,Object> participant(String user,String id,boolean lock) {
        var row=db.one("""
            SELECT s.id,s.vecino_id,s.estado,p.usuario_id proveedor_id
            FROM solicitud s JOIN perfil p ON p.id=s.perfil_id WHERE s.id=?
            """+(lock ? " FOR UPDATE" : ""),id);
        db.allow(user.equals(row.get("vecino_id")) || user.equals(row.get("proveedor_id")));
        return row;
    }
    public void state(String user,String id,EstadoSolicitud state) {
        var row=participant(user,id,true);
        String old=(String)row.get("estado");
        if(state==EstadoSolicitud.FINALIZADA) {
            db.require("ACEPTADA".equals(old),"Solo se finaliza una solicitud aceptada");
            db.update("UPDATE solicitud SET estado='FINALIZADA',finalizada_en=UTC_TIMESTAMP(6) WHERE id=?",id);
        } else {
            db.allow(user.equals(row.get("proveedor_id")));
            db.require("PENDIENTE".equals(old),"La solicitud ya fue respondida");
            db.update("UPDATE solicitud SET estado=? WHERE id=?",state.name(),id);
        }
    }
    public record MessageEvent(String client,String provider,Map<String,Object> message) {}
    public String message(String user,String id,Mensaje m) {
        var row=participant(user,id,true);
        db.require("ACEPTADA".equals(row.get("estado")),"El chat requiere una solicitud aceptada");
        db.update("INSERT INTO mensaje(id,solicitud_id,autor_id,texto,enviado_en) VALUES(?,?,?,?,UTC_TIMESTAMP(6))",
            m.id().toString(),id,user,m.texto());
        var message=db.one("SELECT id,solicitud_id,autor_id,texto,enviado_en FROM mensaje WHERE id=?",m.id().toString());
        events.publishEvent(new MessageEvent((String)row.get("vecino_id"),(String)row.get("proveedor_id"),message));
        return m.id().toString();
    }
    public void rateClient(String user,String id,int value) {
        var row=participant(user,id,true);
        db.allow(user.equals(row.get("proveedor_id")));
        db.require("FINALIZADA".equals(row.get("estado")),"La solicitud debe estar finalizada");
        String client=(String)row.get("vecino_id");
        db.userLock(client);
        db.update("""
            INSERT INTO calificacion_vecino(solicitud_id,valor,creada_en) VALUES(?,?,UTC_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE valor=?,creada_en=UTC_TIMESTAMP(6)
            """,id,value,value);
        Number average=(Number)db.one("""
            SELECT (10.0*3.0+COALESCE(SUM(c.valor),0))/(10.0+COUNT(c.valor)) confianza
            FROM calificacion_vecino c JOIN solicitud s ON s.id=c.solicitud_id WHERE s.vecino_id=?
            """,client).get("confianza");
        db.update("UPDATE usuario SET nivel_confianza=? WHERE id=?",average,client);
        db.verificarPorCalificaciones(client);
    }
}
