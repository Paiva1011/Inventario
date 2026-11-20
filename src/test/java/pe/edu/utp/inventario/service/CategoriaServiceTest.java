package pe.edu.utp.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.inventario.model.Categoria;
import pe.edu.utp.inventario.repository.CategoriaRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria(Long id, String nombre, String descripcion) {
        return Categoria.builder().id(id).nombre(nombre).descripcion(descripcion).build();
    }

    @Test
    void listar_devuelveTodasLasCategorias() {
        when(categoriaRepository.findAll()).thenReturn(
                List.of(categoria(1L, "Bebidas", null), categoria(2L, "Limpieza", null)));

        List<Categoria> resultado = categoriaService.listar();

        assertEquals(2, resultado.size());
        assertEquals("Bebidas", resultado.get(0).getNombre());
    }

    @Test
    void guardar_persisteYDevuelveLaCategoria() {
        Categoria nueva = categoria(null, "Snacks", "Bocaditos");
        Categoria guardada = categoria(7L, "Snacks", "Bocaditos");
        when(categoriaRepository.save(nueva)).thenReturn(guardada);

        Categoria resultado = categoriaService.guardar(nueva);

        assertEquals(7L, resultado.getId());
        verify(categoriaRepository).save(nueva);
    }

    @Test
    void buscarPorId_devuelveLaCategoriaCuandoExiste() {
        Categoria c = categoria(1L, "Bebidas", null);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));

        assertSame(c, categoriaService.buscarPorId(1L));
    }

    @Test
    void buscarPorId_devuelveNullCuandoNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(categoriaService.buscarPorId(99L));
    }

    @Test
    void actualizar_copiaNombreYDescripcionYGuarda() {
        Categoria existente = categoria(1L, "Viejo", "Desc vieja");
        Categoria datos = categoria(null, "Nuevo", "Desc nueva");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(existente)).thenReturn(existente);

        Categoria resultado = categoriaService.actualizar(1L, datos);

        assertEquals(1L, resultado.getId());
        assertEquals("Nuevo", resultado.getNombre());
        assertEquals("Desc nueva", resultado.getDescripcion());
        verify(categoriaRepository).save(existente);
    }

    @Test
    void actualizar_devuelveNullYNoGuardaCuandoNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(categoriaService.actualizar(99L, categoria(null, "X", "Y")));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminar_borraPorId() {
        categoriaService.eliminar(3L);

        verify(categoriaRepository).deleteById(3L);
    }
}
