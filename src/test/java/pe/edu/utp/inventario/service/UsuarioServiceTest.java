package pe.edu.utp.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.inventario.model.Usuario;
import pe.edu.utp.inventario.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario(Long id, String nombre, String correo, String rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    private Usuario valido() {
        return usuario(null, "Ana", "ana@correo.pe", "ADMIN", true);
    }

    /* ---------- listar / buscarPorId ---------- */

    @Test
    void listar_devuelveTodosLosUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(valido(), valido()));

        assertEquals(2, usuarioService.listar().size());
    }

    @Test
    void buscarPorId_devuelveElUsuarioCuandoExiste() {
        Usuario u = usuario(1L, "Ana", "ana@correo.pe", "ADMIN", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));

        assertSame(u, usuarioService.buscarPorId(1L));
    }

    @Test
    void buscarPorId_lanza404CuandoNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.buscarPorId(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    /* ---------- crear ---------- */

    @Test
    void crear_guardaElUsuarioConIdNulo() {
        Usuario entrada = valido();
        entrada.setId(55L); // un id enviado por el cliente debe ignorarse
        when(usuarioRepository.existsByCorreo("ana@correo.pe")).thenReturn(false);
        when(usuarioRepository.save(entrada)).thenReturn(entrada);

        Usuario resultado = usuarioService.crear(entrada);

        assertNull(resultado.getId());
        verify(usuarioRepository).save(entrada);
    }

    @Test
    void crear_conCorreoDuplicadoLanza409YNoGuarda() {
        when(usuarioRepository.existsByCorreo("ana@correo.pe")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.crear(valido()));

        assertEquals(409, ex.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_sinNombreLanza400() {
        Usuario u = valido();
        u.setNombre("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.crear(u));

        assertEquals(400, ex.getStatusCode().value());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void crear_sinCorreoLanza400() {
        Usuario u = valido();
        u.setCorreo(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.crear(u));

        assertEquals(400, ex.getStatusCode().value());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void crear_sinRolLanza400() {
        Usuario u = valido();
        u.setRol("");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.crear(u));

        assertEquals(400, ex.getStatusCode().value());
        verifyNoInteractions(usuarioRepository);
    }

    /* ---------- actualizar ---------- */

    @Test
    void actualizar_copiaLosCamposYGuarda() {
        Usuario existente = usuario(1L, "Ana", "ana@correo.pe", "ADMIN", true);
        Usuario datos = usuario(null, "Ana María", "ana.maria@correo.pe", "CONSULTA", false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByCorreo("ana.maria@correo.pe")).thenReturn(Optional.empty());
        when(usuarioRepository.save(existente)).thenReturn(existente);

        Usuario resultado = usuarioService.actualizar(1L, datos);

        assertEquals(1L, resultado.getId());
        assertEquals("Ana María", resultado.getNombre());
        assertEquals("ana.maria@correo.pe", resultado.getCorreo());
        assertEquals("CONSULTA", resultado.getRol());
        assertFalse(resultado.isActivo());
    }

    @Test
    void actualizar_permiteConservarElPropioCorreo() {
        Usuario existente = usuario(1L, "Ana", "ana@correo.pe", "ADMIN", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByCorreo("ana@correo.pe")).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);

        assertDoesNotThrow(() -> usuarioService.actualizar(1L, valido()));
    }

    @Test
    void actualizar_conCorreoDeOtroUsuarioLanza409() {
        Usuario existente = usuario(1L, "Ana", "ana@correo.pe", "ADMIN", true);
        Usuario otro = usuario(2L, "Luis", "luis@correo.pe", "CONSULTA", true);
        Usuario datos = usuario(null, "Ana", "luis@correo.pe", "ADMIN", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByCorreo("luis@correo.pe")).thenReturn(Optional.of(otro));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(1L, datos));

        assertEquals(409, ex.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_conUsuarioInexistenteLanza404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(99L, valido()));

        assertEquals(404, ex.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizar_conDatosInvalidosLanza400() {
        Usuario existente = usuario(1L, "Ana", "ana@correo.pe", "ADMIN", true);
        Usuario datos = usuario(null, "", "ana@correo.pe", "ADMIN", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.actualizar(1L, datos));

        assertEquals(400, ex.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    /* ---------- eliminar ---------- */

    @Test
    void eliminar_borraElUsuarioExistente() {
        Usuario existente = usuario(1L, "Ana", "ana@correo.pe", "ADMIN", true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        usuarioService.eliminar(1L);

        verify(usuarioRepository).delete(existente);
    }

    @Test
    void eliminar_conUsuarioInexistenteLanza404YNoBorra() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> usuarioService.eliminar(99L));

        assertEquals(404, ex.getStatusCode().value());
        verify(usuarioRepository, never()).delete(any());
    }
}
