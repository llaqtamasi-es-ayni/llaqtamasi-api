package pe.llaqtamasi.controller;
import pe.llaqtamasi.service.AdminService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import static pe.llaqtamasi.dto.Requests.*;

@RestController
@RequestMapping("/api")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }
    @GetMapping("/catalogos/{tipo}")
    public List<Map<String,Object>> catalog(@AuthenticationPrincipal Jwt jwt,@PathVariable Catalog tipo) {
        return service.catalog(jwt, tipo);
    }
    @PostMapping("/catalogos/{tipo}")
    public Map<String,String> catalog(@AuthenticationPrincipal Jwt jwt,@PathVariable Catalog tipo,@Valid @RequestBody Catalogo c) {
        return service.catalog(jwt, tipo, c);
    }
    @PutMapping("/admin/usuarios/{usuarioId}/roles/{tipo}/{rolId}")
    public void assign(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID usuarioId,@PathVariable Catalog tipo,@PathVariable UUID rolId) {
        service.assign(jwt, usuarioId, tipo, rolId);
    }
    @DeleteMapping("/admin/usuarios/{usuarioId}/roles/{tipo}/{rolId}")
    public void revoke(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID usuarioId,@PathVariable Catalog tipo,@PathVariable UUID rolId) {
        service.revoke(jwt, usuarioId, tipo, rolId);
    }
    @PatchMapping("/admin/usuarios/{id}/verificar")
    public void verify(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        service.verify(jwt, id);
    }
    @GetMapping("/zonas")
    public List<Map<String,Object>> zones(@AuthenticationPrincipal Jwt jwt) {
        return service.zones(jwt);
    }
    @PostMapping("/zonas")
    public void zone(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Zona z) {
        service.zone(jwt, z);
    }
    @GetMapping("/centros")
    public List<Map<String,Object>> centers(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID zonaId) {
        return service.centers(jwt, zonaId);
    }
    @PostMapping("/centros")
    public void center(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Centro c) {
        service.center(jwt, c);
    }
    @PostMapping("/paquetes")
    public void packageMap(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Paquete p) {
        service.packageMap(jwt, p);
    }
    @GetMapping("/zonas/{id}/paquete")
    public Map<String,Object> packageMap(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        return service.packageMap(jwt, id);
    }
}
