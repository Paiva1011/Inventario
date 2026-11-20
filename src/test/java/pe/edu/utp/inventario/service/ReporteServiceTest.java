package pe.edu.utp.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.inventario.dto.StockBajoResponse;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.repository.MovimientoInventarioRepository;
import pe.edu.utp.inventario.repository.ProductoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    void obtenerProductosStockBajo_usaElUmbralDeDiezUnidades() {
        when(productoRepository.findByStockLessThan(10)).thenReturn(List.of());

        reporteService.obtenerProductosStockBajo();

        verify(productoRepository).findByStockLessThan(10);
    }

    @Test
    void obtenerProductosStockBajo_convierteCadaProductoEnUnaAlerta() {
        Producto bajo = Producto.builder().id(1L).codigo("P-001").nombre("Agua").stock(4).build();
        when(productoRepository.findByStockLessThan(10)).thenReturn(List.of(bajo));

        List<StockBajoResponse> resultado = reporteService.obtenerProductosStockBajo();

        assertEquals(1, resultado.size());
        StockBajoResponse alerta = resultado.get(0);
        assertEquals(1L, alerta.getId());
        assertEquals("Agua", alerta.getNombre());
        assertEquals(4, alerta.getStock());
        assertEquals("STOCK BAJO", alerta.getEstado());
        assertEquals("Reabastecer producto inmediatamente", alerta.getAlerta());
    }

    @Test
    void obtenerProductosStockBajo_sinProductosDevuelveListaVacia() {
        when(productoRepository.findByStockLessThan(10)).thenReturn(List.of());

        assertTrue(reporteService.obtenerProductosStockBajo().isEmpty());
    }

    @Test
    void obtenerTotalProductos_devuelveLaCuenta() {
        when(productoRepository.count()).thenReturn(42L);

        assertEquals(42L, reporteService.obtenerTotalProductos());
    }

    @Test
    void obtenerInventario_devuelveTodosLosProductos() {
        when(productoRepository.findAll()).thenReturn(List.of(new Producto(), new Producto()));

        assertEquals(2, reporteService.obtenerInventario().size());
    }

    @Test
    void obtenerMovimientos_devuelveTodosLosMovimientos() {
        when(movimientoRepository.findAll()).thenReturn(List.of(new MovimientoInventario()));

        assertEquals(1, reporteService.obtenerMovimientos().size());
    }
}
