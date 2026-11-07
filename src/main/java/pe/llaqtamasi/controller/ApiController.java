package pe.llaqtamasi.controller;
import pe.llaqtamasi.service.ConsultaService;
import pe.llaqtamasi.service.CommunityService;
import pe.llaqtamasi.service.SolicitudService;
import pe.llaqtamasi.security.Access;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import static pe.llaqtamasi.dto.Requests.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final Access access;
    private final ConsultaService queries;
    private final CommunityService community;
    private final SolicitudService requests;
    public ApiController(Access access,ConsultaService queries,CommunityService community,SolicitudService requests) {
        this.access=access; this.queries=queries; this.community=community; this.requests=requests;
    }
    @GetMapping("/perfiles")
    public List<Map<String,Object>> profiles(@AuthenticationPrincipal Jwt jwt,@RequestParam double lat,@RequestParam double lng,
        @RequestParam(required=false) TipoPerfil tipo) {
        return queries.profiles(jwt, lat, lng, tipo);
    }
    @PostMapping("/perfiles")
    public Map<String,String> profile(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Perfil p) {
        return Map.of("id",community.profile(access.user(jwt),p));
    }
    @PatchMapping("/perfiles/{id}/visibilidad")
    public void visible(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody Visible p) {
        community.visibility(access.user(jwt),id.toString(),p.visible());
    }
    @PostMapping("/usuarios/{id}/avales")
    public void endorse(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        community.endorse(access.user(jwt),id.toString());
    }
    @PutMapping("/perfiles/{id}/voto")
    public void vote(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Voto v) {
        community.vote(access.user(jwt),id.toString(),v.valor());
    }
    @PostMapping("/solicitudes")
    public Map<String,String> request(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Solicitud s) {
        return Map.of("id",requests.create(access.user(jwt),s));
    }
    @GetMapping("/solicitudes")
    public List<Map<String,Object>> requests(@AuthenticationPrincipal Jwt jwt) {
        return queries.requests(jwt);
    }
    @PatchMapping("/solicitudes/{id}")
    public void state(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Estado state) {
        requests.state(access.user(jwt),id.toString(),state.estado());
    }
    @PostMapping("/solicitudes/{id}/mensajes")
    public Map<String,String> message(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Mensaje m) {
        return Map.of("id",requests.message(access.user(jwt),id.toString(),m));
    }
    @GetMapping("/solicitudes/{id}/mensajes")
    public List<Map<String,Object>> messages(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
        @RequestParam(defaultValue="0") int offset) {
        return queries.messages(jwt, id, offset);
    }
    @PutMapping("/solicitudes/{id}/calificacion-vecino")
    public void rate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Voto v) {
        requests.rateClient(access.user(jwt),id.toString(),v.valor());
    }
    @PostMapping("/muro")
    public Map<String,String> wall(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Muro p) {
        return Map.of("id",community.wall(access.user(jwt),p));
    }
    @GetMapping("/muro")
    public List<Map<String,Object>> wall(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID zonaId) {
        return queries.wall(jwt, zonaId);
    }
    @PatchMapping("/muro/{id}/cerrar")
    public void close(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        community.closeWall(access.user(jwt),id.toString());
    }
    @PostMapping("/reportes")
    public Map<String,String> report(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Reporte r) {
        return Map.of("id",community.report(access.user(jwt),r));
    }
    @GetMapping("/reportes")
    public List<Map<String,Object>> reports(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID zonaId) {
        return queries.reports(jwt, zonaId);
    }
    @PutMapping("/reportes/{id}/respuesta")
    public void response(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Respuesta r) {
        community.respond(access.user(jwt),id.toString(),r.decision());
    }
}
