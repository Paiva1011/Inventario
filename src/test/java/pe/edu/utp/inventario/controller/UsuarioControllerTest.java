package pe.edu.utp.inventario.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.inventario.model.Usuario;
import pe.edu.utp.inventario.service.UsuarioService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private static final String BODY = """
            {"nombre":"Ana","correo":"ana@correo.pe","rol":"ADMIN","activo":false}
            """;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(usuarioController).build();
    }

    private Usuario usuario(Long id, String nombre, String correo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setRol("ADMIN");
        u.setActivo(true);
        return u;
    }

    @Test
    void listar_responde200ConLosUsuarios() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(
                usuario(1L, "Ana", "ana@correo.pe"), usuario(2L, "Luis", "luis@correo.pe")));

        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].correo").value("ana@correo.pe"))
                .andExpect(jsonPath("$[1].activo").value(true));
    }

    @Test
    void obtener_responde200ConElUsuario() throws Exception {
        when(usuarioService.buscarPorId(1L)).thenReturn(usuario(1L, "Ana", "ana@correo.pe"));

        mvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void obtener_inexistenteResponde404() throws Exception {
        when(usuarioService.buscarPorId(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: 99"));

        mvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_responde201YEnviaElUsuarioDelJsonAlServicio() throws Exception {
        when(usuarioService.crear(any(Usuario.class))).thenReturn(usuario(3L, "Ana", "ana@correo.pe"));

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).crear(captor.capture());
        assertEquals("Ana", captor.getValue().getNombre());
        assertEquals("ana@correo.pe", captor.getValue().getCorreo());
        assertEquals("ADMIN", captor.getValue().getRol());
        assertFalse(captor.getValue().isActivo());
    }

    @Test
    void crear_conCorreoDuplicadoResponde409() throws Exception {
        when(usuarioService.crear(any(Usuario.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado"));

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizar_usaElIdDeLaRuta() throws Exception {
        when(usuarioService.actualizar(eq(2L), any(Usuario.class)))
                .thenReturn(usuario(2L, "Ana", "ana@correo.pe"));

        mvc.perform(put("/api/usuarios/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void eliminar_responde204() throws Exception {
        mvc.perform(delete("/api/usuarios/4"))
                .andExpect(status().isNoContent());

        verify(usuarioService).eliminar(4L);
    }
}
