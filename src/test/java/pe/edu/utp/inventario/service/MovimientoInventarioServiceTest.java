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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private MovimientoInventarioService service;

    private Producto producto(Long id, int stock) {
        return Producto.builder().id(id).codigo("P-" + id).nombre("Producto " + id)
                .precio(2.0).stock(stock).build();
    }

    private MovimientoInventario movimiento(String tipo, int cantidad, Producto producto) {
        return MovimientoInventario.builder().tipo(tipo).cantidad(cantidad).producto(producto).build();
    }

    @Test
    void listar_devuelveTodosLosMovimientos() {
        when(movimientoInventarioRepository.findAll()).thenReturn(
                List.of(movimiento("ENTRADA", 5, producto(1L, 0))));

        assertEquals(1, service.listar().size());
    }

    @Test
    void guardar_entradaSumaAlStockYGuardaElMovimiento() {
        Producto enBd = producto(1L, 10);
        MovimientoInventario mov = movimiento("ENTRADA", 5, producto(1L, 0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(enBd));
        when(movimientoInventarioRepository.save(mov)).thenReturn(mov);

        MovimientoInventario resultado = service.guardar(mov);

        assertEquals(15, enBd.getStock());
        verify(productoRepository).save(enBd);
        assertSame(mov, resultado);
    }

    @Test
    void guardar_salidaRestaDelStock() {
        Producto enBd = producto(1L, 10);
        MovimientoInventario mov = movimiento("SALIDA", 4, producto(1L, 0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(enBd));
        when(movimientoInventarioRepository.save(mov)).thenReturn(mov);

        service.guardar(mov);

        assertEquals(6, enBd.getStock());
        verify(productoRepository).save(enBd);
        verify(movimientoInventarioRepository).save(mov);
    }

    @Test
    void guardar_salidaPorTodoElStockDisponibleEsValida() {
        Producto enBd = producto(1L, 7);
        MovimientoInventario mov = movimiento("SALIDA", 7, producto(1L, 0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(enBd));
        when(movimientoInventarioRepository.save(mov)).thenReturn(mov);

        service.guardar(mov);

        assertEquals(0, enBd.getStock());
    }

    @Test
    void guardar_ignoraMayusculasEnElTipo() {
        Producto enBd = producto(1L, 10);
        MovimientoInventario mov = movimiento("entrada", 3, producto(1L, 0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(enBd));
        when(movimientoInventarioRepository.save(mov)).thenReturn(mov);

        service.guardar(mov);

        assertEquals(13, enBd.getStock());
    }

    @Test
    void guardar_salidaConStockInsuficienteLanzaExcepcionYNoGuardaNada() {
        Producto enBd = producto(1L, 3);
        MovimientoInventario mov = movimiento("SALIDA", 4, producto(1L, 0));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(enBd));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.guardar(mov));

        assertEquals("Stock insuficiente", ex.getMessage());
        assertEquals(3, enBd.getStock());
        verify(productoRepository, never()).save(any());
        verifyNoInteractions(movimientoInventarioRepository);
    }

    @Test
    void guardar_conProductoInexistenteLanzaExcepcion() {
        MovimientoInventario mov = movimiento("ENTRADA", 1, producto(99L, 0));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.guardar(mov));

        assertEquals("Producto no encontrado", ex.getMessage());
        verify(productoRepository, never()).save(any());
        verifyNoInteractions(movimientoInventarioRepository);
    }

    @Test
    void buscarPorId_devuelveElMovimientoONullSiNoExiste() {
        MovimientoInventario mov = movimiento("ENTRADA", 1, producto(1L, 0));
        when(movimientoInventarioRepository.findById(1L)).thenReturn(Optional.of(mov));
        when(movimientoInventarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertSame(mov, service.buscarPorId(1L));
        assertNull(service.buscarPorId(2L));
    }

    @Test
    void eliminar_borraPorIdSinTocarElStock() {
        service.eliminar(5L);

        verify(movimientoInventarioRepository).deleteById(5L);
        verifyNoInteractions(productoRepository);
    }
}
