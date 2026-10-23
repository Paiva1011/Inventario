package pe.edu.utp.inventario.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.inventario.model.Usuario;
import pe.edu.utp.inventario.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado: " + id));
    }

    @Transactional
    public Usuario crear(Usuario usuario) {
        validar(usuario);
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        usuario.setId(null);
        return usuarioRepository.save(usuario);
    }
        @Transactional
    public Usuario actualizar(Long id, Usuario datos) {
        Usuario existente = buscarPorId(id);
        validar(datos);
        usuarioRepository.findByCorreo(datos.getCorreo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
                });
        existente.setNombre(datos.getNombre());
        existente.setCorreo(datos.getCorreo());
        existente.setRol(datos.getRol());
        existente.setActivo(datos.isActivo());
        return usuarioRepository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario existente = buscarPorId(id);
        usuarioRepository.delete(existente);
    }

    private void validar(Usuario usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }
        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
    }
}