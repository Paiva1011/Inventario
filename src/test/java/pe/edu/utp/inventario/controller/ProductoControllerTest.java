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
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.service.ProductoService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(productoController).build();
    }

    private Producto producto(Long id, String codigo, String nombre) {
        return Producto.builder().id(id).codigo(codigo).nombre(nombre).precio(1.5).stock(10).build();
    }

    @Test
    void listar_responde200ConLaListaEnJson() throws Exception {
        when(productoService.listar()).thenReturn(List.of(
                producto(1L, "P-001", "Agua"), producto(2L, "P-002", "Gaseosa")));

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("P-001"))
                .andExpect(jsonPath("$[1].nombre").value("Gaseosa"));
    }

    @Test
    void buscarPorId_responde200ConElProducto() throws Exception {
        when(productoService.buscarPorId(1L)).thenReturn(producto(1L, "P-001", "Agua"));

        mvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.precio").value(1.5))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void buscarPorId_inexistenteResponde200SinCuerpo() throws Exception {
        // Comportamiento actual: el servicio devuelve null y el controlador no lo traduce a 404.
        when(productoService.buscarPorId(99L)).thenReturn(null);

        mvc.perform(get("/api/productos/99"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void guardar_enviaElProductoDelJsonAlServicio() throws Exception {
        when(productoService.guardar(any(Producto.class))).thenReturn(producto(5L, "P-005", "Leche"));

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"P-005","nombre":"Leche","precio":4.2,"stock":12,"categoria":{"id":3}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoService).guardar(captor.capture());
        Producto recibido = captor.getValue();
        assertEquals("P-005", recibido.getCodigo());
        assertEquals("Leche", recibido.getNombre());
        assertEquals(4.2, recibido.getPrecio());
        assertEquals(12, recibido.getStock());
        assertEquals(3L, recibido.getCategoria().getId());
    }

    @Test
    void actualizar_usaElIdDeLaRuta() throws Exception {
        when(productoService.actualizar(eq(7L), any(Producto.class)))
                .thenReturn(producto(7L, "P-007", "Editado"));

        mvc.perform(put("/api/productos/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"P-007\",\"nombre\":\"Editado\",\"precio\":2,\"stock\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Editado"));
    }

    @Test
    void eliminar_delegaEnElServicio() throws Exception {
        mvc.perform(delete("/api/productos/4"))
                .andExpect(status().isOk());

        verify(productoService).eliminar(4L);
    }

    @Test
    void buscarPorNombre_pasaElParametroAlServicio() throws Exception {
        when(productoService.buscarPorNombre("ag")).thenReturn(List.of(producto(1L, "P-001", "Agua")));

        mvc.perform(get("/api/productos/buscar").param("nombre", "ag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Agua"));
    }

    @Test
    void buscarPorNombre_sinParametroResponde400() throws Exception {
        mvc.perform(get("/api/productos/buscar"))
                .andExpect(status().isBadRequest());
    }
}
