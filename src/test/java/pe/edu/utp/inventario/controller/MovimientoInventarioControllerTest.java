package pe.edu.utp.inventario.controller;

import jakarta.servlet.ServletException;
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
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.service.MovimientoInventarioService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioControllerTest {

    private static final String BODY = """
            {"tipo":"SALIDA","cantidad":4,"producto":{"id":1}}
            """;

    @Mock
    private MovimientoInventarioService movimientoService;

    @InjectMocks
    private MovimientoInventarioController controller;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MovimientoInventario movimiento(Long id, String tipo, int cantidad) {
        Producto p = Producto.builder().id(1L).nombre("Agua").build();
        return MovimientoInventario.builder().id(id).tipo(tipo).cantidad(cantidad).producto(p).build();
    }

    @Test
    void listar_responde200ConLosMovimientos() throws Exception {
        when(movimientoService.listar()).thenReturn(List.of(
                movimiento(1L, "ENTRADA", 10), movimiento(2L, "SALIDA", 3)));

        mvc.perform(get("/api/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$[1].producto.nombre").value("Agua"));
    }

    @Test
    void buscarPorId_responde200ConElMovimiento() throws Exception {
        when(movimientoService.buscarPorId(1L)).thenReturn(movimiento(1L, "ENTRADA", 10));

        mvc.perform(get("/api/movimientos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(10));
    }

    @Test
    void guardar_enviaElMovimientoDelJsonAlServicio() throws Exception {
        when(movimientoService.guardar(any(MovimientoInventario.class)))
                .thenReturn(movimiento(8L, "SALIDA", 4));

        mvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoService).guardar(captor.capture());
        assertEquals("SALIDA", captor.getValue().getTipo());
        assertEquals(4, captor.getValue().getCantidad());
        assertEquals(1L, captor.getValue().getProducto().getId());
    }

    @Test
    void guardar_conStockInsuficientePropagaLaExcepcionDelServicio() {
        // Comportamiento actual: no hay @ControllerAdvice, el RuntimeException del servicio no se traduce a 4xx.
        when(movimientoService.guardar(any(MovimientoInventario.class)))
                .thenThrow(new RuntimeException("Stock insuficiente"));

        ServletException ex = assertThrows(ServletException.class, () ->
                mvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)));

        assertEquals("Stock insuficiente", ex.getCause().getMessage());
    }

    @Test
    void eliminar_delegaEnElServicio() throws Exception {
        mvc.perform(delete("/api/movimientos/6"))
                .andExpect(status().isOk());

        verify(movimientoService).eliminar(6L);
    }
}
