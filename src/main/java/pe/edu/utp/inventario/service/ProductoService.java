package pe.edu.utp.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.repository.ProductoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    public Producto actualizar(Long id, Producto producto) {

        Producto productoExistente =
                productoRepository.findById(id).orElse(null);

        if (productoExistente == null) {
            return null;
        }

        productoExistente.setCodigo(producto.getCodigo());
        productoExistente.setNombre(producto.getNombre());
        productoExistente.setPrecio(producto.getPrecio());
        productoExistente.setStock(producto.getStock());
        productoExistente.setCategoria(producto.getCategoria());

        return productoRepository.save(productoExistente);
    }

    public void eliminar(Long id) {
        productoRepository.deleteById(id);
    }
}