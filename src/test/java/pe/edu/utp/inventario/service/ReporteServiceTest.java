package pe.edu.utp.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.repository.MovimientoInventarioRepository;
import pe.edu.utp.inventario.repository.ProductoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Producto bajo = Producto.builder().id(1L).nombre("Agua").stock(4).build();
        when(productoRepository.findByStockLessThan(10)).thenReturn(List.of(bajo));

        List<Producto> resultado = reporteService.obtenerProductosStockBajo();

        assertEquals(1, resultado.size());
        assertSame(bajo, resultado.get(0));
        verify(productoRepository).findByStockLessThan(10);
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
