package pe.llaqtamasi.repository;
import pe.llaqtamasi.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface Usuarios extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByNombreAcceso(String nombreAcceso);
}
