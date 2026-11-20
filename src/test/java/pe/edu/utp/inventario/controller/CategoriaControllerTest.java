package pe.edu.utp.inventario.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.utp.inventario.model.Categoria;
import pe.edu.utp.inventario.service.CategoriaService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaService categoriaService;

    @InjectMocks
    private CategoriaController categoriaController;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(categoriaController).build();
    }

    private Categoria categoria(Long id, String nombre, String descripcion) {
        return Categoria.builder().id(id).nombre(nombre).descripcion(descripcion).build();
    }

    @Test
    void listar_responde200ConLasCategorias() throws Exception {
        when(categoriaService.listar()).thenReturn(List.of(
                categoria(1L, "Bebidas", "Líquidos"), categoria(2L, "Limpieza", null)));

        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Bebidas"));
    }

    @Test
    void buscarPorId_responde200ConLaCategoria() throws Exception {
        when(categoriaService.buscarPorId(1L)).thenReturn(categoria(1L, "Bebidas", "Líquidos"));

        mvc.perform(get("/api/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Líquidos"));
    }

    @Test
    void guardar_enviaLaCategoriaDelJsonAlServicio() throws Exception {
        when(categoriaService.guardar(any(Categoria.class))).thenReturn(categoria(9L, "Snacks", "Bocaditos"));

        mvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Snacks\",\"descripcion\":\"Bocaditos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaService).guardar(captor.capture());
        assertEquals("Snacks", captor.getValue().getNombre());
        assertEquals("Bocaditos", captor.getValue().getDescripcion());
    }

    @Test
    void actualizar_usaElIdDeLaRuta() throws Exception {
        when(categoriaService.actualizar(eq(2L), any(Categoria.class)))
                .thenReturn(categoria(2L, "Hogar", null));

        mvc.perform(put("/api/categorias/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Hogar\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hogar"));
    }

    @Test
    void eliminar_delegaEnElServicio() throws Exception {
        mvc.perform(delete("/api/categorias/3"))
                .andExpect(status().isOk());

        verify(categoriaService).eliminar(3L);
    }
}
