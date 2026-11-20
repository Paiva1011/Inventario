package pe.edu.utp.inventario.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.utp.inventario.dto.StockBajoResponse;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.service.ReporteService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReporteService reporteService;

    @InjectMocks
    private ReporteController reporteController;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(reporteController).build();
    }

    @Test
    void stockBajo_devuelveLasAlertasDeProductosConPocoStock() throws Exception {
        when(reporteService.obtenerProductosStockBajo()).thenReturn(List.of(
                new StockBajoResponse(1L, "Agua", 4, "STOCK BAJO", "Reabastecer producto inmediatamente")));

        mvc.perform(get("/api/reportes/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Agua"))
                .andExpect(jsonPath("$[0].stock").value(4))
                .andExpect(jsonPath("$[0].estado").value("STOCK BAJO"))
                .andExpect(jsonPath("$[0].alerta").value("Reabastecer producto inmediatamente"));
    }

    @Test
    void totalProductos_devuelveUnObjetoConLaClaveTotal() throws Exception {
        when(reporteService.obtenerTotalProductos()).thenReturn(42L);

        mvc.perform(get("/api/reportes/total-productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(42));
    }

    @Test
    void inventario_devuelveTodosLosProductos() throws Exception {
        when(reporteService.obtenerInventario()).thenReturn(List.of(
                Producto.builder().id(1L).nombre("Agua").build(),
                Producto.builder().id(2L).nombre("Gaseosa").build()));

        mvc.perform(get("/api/reportes/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void movimientos_devuelveElHistorial() throws Exception {
        when(reporteService.obtenerMovimientos()).thenReturn(List.of(
                MovimientoInventario.builder().id(1L).tipo("ENTRADA").cantidad(10).build()));

        mvc.perform(get("/api/reportes/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"));
    }
}
