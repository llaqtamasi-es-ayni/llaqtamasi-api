package pe.llaqtamasi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public class Requests {
    public enum Catalog { categorias, rolesComunitarios, rolesSistema }
    public record Registro(@NotBlank @Size(max=150) String alias,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,100}") String nombreAcceso,
        @NotBlank @Size(min=12,max=72) String password) {}
    public record Login(@NotBlank @Size(max=100) String nombreAcceso,
        @NotBlank @Size(max=72) String password) {}
    public enum TipoPerfil { SERVICIO, TIENDA, APOYO }
    public enum EstadoSolicitud { ACEPTADA, RECHAZADA, FINALIZADA }
    public enum TipoMuro { OFREZCO, NECESITO }
    public enum Decision { CONFIRMA, DESMIENTE }
    public enum TipoCentro { COMEDOR, OLLA_COMUN, ADULTO_MAYOR, NINOS, SALUD, COMISARIA, ACOPIO }
    public record Catalogo(@NotBlank @Size(max=150) String nombre, @Size(max=2000) String descripcion) {}
    public record Perfil(@NotNull UUID id, @NotNull UUID categoriaId, @NotNull TipoPerfil tipo,
        @NotBlank @Size(max=4000) String descripcion, @NotBlank @Size(max=50000) String areaWkt) {}
    public record Visible(boolean visible) {}
    public record Solicitud(@NotNull UUID id, @NotNull UUID perfilId, @NotBlank @Size(max=2000) String mensaje) {}
    public record Estado(@NotNull EstadoSolicitud estado) {}
    public record Mensaje(@NotNull UUID id, @NotBlank @Size(max=4000) String texto) {}
    public record Voto(@Min(1) @Max(5) int valor) {}
    public record Muro(@NotNull UUID id, @NotNull UUID zonaId, @NotNull TipoMuro tipo,
        @NotBlank @Size(max=150) String categoria, @NotBlank @Size(max=4000) String texto) {}
    public record Reporte(@NotNull UUID id, @NotBlank @Size(max=150) String tipo,
        @DecimalMin("-90") @DecimalMax("90") double lat,
        @DecimalMin("-180") @DecimalMax("180") double lng,
        @NotBlank @Size(max=2000) String texto, @Min(1) @Max(24) int duracionHoras) {}
    public record Respuesta(@NotNull Decision decision) {}
    public record Zona(@NotNull UUID id, @NotBlank @Size(max=150) String nombre,
        @NotBlank @Size(max=50000) String limiteWkt) {}
    public record Centro(@NotNull UUID id, @NotNull UUID zonaId, @NotBlank @Size(max=150) String nombre,
        @NotNull TipoCentro tipo, @DecimalMin("-90") @DecimalMax("90") double lat,
        @DecimalMin("-180") @DecimalMax("180") double lng,
        @Size(max=150) String horario, @Size(max=150) String contactoPublico) {}
    public record Paquete(@NotNull UUID id, @NotNull UUID zonaId, @Min(1) int version,
        @NotBlank @Size(max=2000) String referenciaArchivo) {}
    public enum Operacion { PERFIL, SOLICITUD, ESTADO_SOLICITUD, MENSAJE, VOTO, AVAL, CALIFICACION, MURO, CERRAR_MURO, REPORTE, RESPUESTA, VISIBILIDAD }
    public record Accion(@NotNull UUID idAccion, @NotNull Operacion tipo, UUID destinoId,
        @Valid Perfil perfil, @Valid Solicitud solicitud, @Valid Estado estado, @Valid Mensaje mensaje,
        @Valid Voto voto, @Valid Muro muro, @Valid Reporte reporte, @Valid Respuesta respuesta,
        @Valid Visible visibilidad) {}
    public record Lote(@NotEmpty @Size(max=100) List<@Valid Accion> acciones) {}
}
