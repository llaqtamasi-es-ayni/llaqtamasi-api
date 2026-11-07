package pe.llaqtamasi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id public String id;
    public String alias;
    @Column(name = "nombre_acceso") public String nombreAcceso;
    @Column(name = "password_hash") public String passwordHash;
    public boolean activo;
    public boolean verificado;
    @Column(name = "nivel_confianza") public BigDecimal nivelConfianza = BigDecimal.ZERO;
    @Column(name = "registrado_en") public LocalDateTime registradoEn;
}
