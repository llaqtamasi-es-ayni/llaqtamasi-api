package pe.llaqtamasi.service;
import pe.llaqtamasi.repository.Db;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static pe.llaqtamasi.dto.Requests.*;

@Service
public class SyncService {
    private final Db db;
    private final CommunityService community;
    private final SolicitudService requests;
    public SyncService(Db db,CommunityService community,SolicitudService requests) {
        this.db=db; this.community=community; this.requests=requests;
    }
    @Transactional
    public Map<String,Object> apply(String user,Accion a) {
        db.userLock(user);
        String action=a.idAccion().toString();
        var prior=db.list("SELECT usuario_id FROM accion_sync WHERE id_accion=?",action);
        if(!prior.isEmpty()) {
            db.allow(user.equals(prior.getFirst().get("usuario_id")));
            return Map.of("idAccion",action,"estado","YA_APLICADA");
        }
        // Reserve and mutate in one transaction. Failed operations roll back both.
        db.update("INSERT INTO accion_sync(id_accion,usuario_id,aplicada_en) VALUES(?,?,UTC_TIMESTAMP(6))",action,user);
        String id=a.destinoId()==null?null:a.destinoId().toString();
        switch(a.tipo()) {
            case PERFIL -> community.profile(user,required(a.perfil()));
            case SOLICITUD -> requests.create(user,required(a.solicitud()));
            case ESTADO_SOLICITUD -> requests.state(user,required(id),required(a.estado()).estado());
            case MENSAJE -> requests.message(user,required(id),required(a.mensaje()));
            case VOTO -> community.vote(user,required(id),required(a.voto()).valor());
            case AVAL -> community.endorse(user,required(id));
            case CALIFICACION -> requests.rateClient(user,required(id),required(a.voto()).valor());
            case MURO -> community.wall(user,required(a.muro()));
            case CERRAR_MURO -> community.closeWall(user,required(id));
            case REPORTE -> community.report(user,required(a.reporte()));
            case RESPUESTA -> community.respond(user,required(id),required(a.respuesta()).decision());
            case VISIBILIDAD -> community.visibility(user,required(id),required(a.visibilidad()).visible());
        }
        return Map.of("idAccion",action,"estado","APLICADA");
    }
    private <T> T required(T value) {
        db.require(value!=null,"Falta el contenido de la accion");
        return value;
    }
}
