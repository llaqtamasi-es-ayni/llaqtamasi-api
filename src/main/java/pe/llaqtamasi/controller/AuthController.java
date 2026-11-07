package pe.llaqtamasi.controller;
import pe.llaqtamasi.service.AuthService;
import jakarta.validation.Valid;
import static pe.llaqtamasi.dto.Requests.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.*;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }
    @PostMapping("/auth/registro")
    @ResponseStatus(CREATED)
    public Map<String,Object> register(@Valid @RequestBody Registro request) {
        return service.register(request);
    }
    @PostMapping("/auth/login")
    public Map<String,Object> login(@Valid @RequestBody Login request) {
        return service.login(request);
    }
    @GetMapping("/usuarios/me")
    public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(jwt);
    }
    @GetMapping("/salud")
    public Map<String,String> health() {
        return service.health();
    }
}
