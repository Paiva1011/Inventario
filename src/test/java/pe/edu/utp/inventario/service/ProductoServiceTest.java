package pe.edu.utp.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.inventario.model.Categoria;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.repository.ProductoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto(Long id, String codigo, String nombre, double precio, int stock, Categoria categoria) {
        return Producto.builder().id(id).codigo(codigo).nombre(nombre)
                .precio(precio).stock(stock).categoria(categoria).build();
    }

    @Test
    void listar_devuelveTodosLosProductos() {
        when(productoRepository.findAll()).thenReturn(List.of(
                producto(1L, "P-001", "Agua", 1.5, 10, null),
                producto(2L, "P-002", "Gaseosa", 3.0, 5, null)));

        assertEquals(2, productoService.listar().size());
    }

    @Test
    void guardar_persisteYDevuelveElProducto() {
        Producto nuevo = producto(null, "P-001", "Agua", 1.5, 10, null);
        Producto guardado = producto(1L, "P-001", "Agua", 1.5, 10, null);
        when(productoRepository.save(nuevo)).thenReturn(guardado);

        assertEquals(1L, productoService.guardar(nuevo).getId());
    }

    @Test
    void buscarPorId_devuelveElProductoCuandoExiste() {
        Producto p = producto(1L, "P-001", "Agua", 1.5, 10, null);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));

        assertSame(p, productoService.buscarPorId(1L));
    }

    @Test
    void buscarPorId_devuelveNullCuandoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(productoService.buscarPorId(99L));
    }

    @Test
    void actualizar_copiaTodosLosCamposYGuarda() {
        Categoria categoria = Categoria.builder().id(2L).nombre("Limpieza").build();
        Producto existente = producto(1L, "P-001", "Agua", 1.5, 10, null);
        Producto datos = producto(null, "P-999", "Detergente", 12.9, 40, categoria);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(existente)).thenReturn(existente);

        Producto resultado = productoService.actualizar(1L, datos);

        assertEquals(1L, resultado.getId());
        assertEquals("P-999", resultado.getCodigo());
        assertEquals("Detergente", resultado.getNombre());
        assertEquals(12.9, resultado.getPrecio());
        assertEquals(40, resultado.getStock());
        assertSame(categoria, resultado.getCategoria());
    }

    @Test
    void actualizar_devuelveNullYNoGuardaCuandoNoExiste() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(productoService.actualizar(99L, producto(null, "X", "Y", 1, 1, null)));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void eliminar_borraPorId() {
        productoService.eliminar(4L);

        verify(productoRepository).deleteById(4L);
    }

    @Test
    void buscarPorNombre_delegaEnElRepositorio() {
        Producto agua = producto(1L, "P-001", "Agua", 1.5, 10, null);
        when(productoRepository.findByNombreContaining("Ag")).thenReturn(List.of(agua));

        List<Producto> resultado = productoService.buscarPorNombre("Ag");

        assertEquals(1, resultado.size());
        assertSame(agua, resultado.get(0));
    }
}
