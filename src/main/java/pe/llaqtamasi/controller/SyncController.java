package pe.llaqtamasi.controller;
import pe.llaqtamasi.service.SyncService;
import pe.llaqtamasi.security.Access;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.dao.DataAccessException;
import org.springframework.web.server.ResponseStatusException;
import static pe.llaqtamasi.dto.Requests.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final Access access;
    private final SyncService sync;
    public SyncController(Access access,SyncService sync) { this.access=access; this.sync=sync; }
    @PostMapping
    public List<Map<String,Object>> apply(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Lote batch) {
        String user=access.user(jwt);
        List<Map<String,Object>> result=new ArrayList<>();
        for(Accion action:batch.acciones()) {
            try { result.add(sync.apply(user,action)); }
            catch(ResponseStatusException e) {
                result.add(Map.of("idAccion",action.idAccion(),"estado","ERROR","codigo",e.getStatusCode().value(),"error",Objects.toString(e.getReason(),"Accion rechazada")));
            } catch(DataAccessException e) {
                result.add(Map.of("idAccion",action.idAccion(),"estado","ERROR","codigo",409,"error","Conflicto de datos; vuelve a sincronizar"));
            }
        }
        return result;
    }
}
