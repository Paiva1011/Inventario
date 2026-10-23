package pe.edu.utp.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.repository.MovimientoInventarioRepository;
import pe.edu.utp.inventario.repository.ProductoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoRepository productoRepository;

    public List<MovimientoInventario> listar() {
        return movimientoInventarioRepository.findAll();
    }

    public MovimientoInventario guardar(MovimientoInventario movimiento) {

        Producto producto = productoRepository
                .findById(movimiento.getProducto().getId())
                .orElseThrow(() ->
                        new RuntimeException("Producto no encontrado"));

        if ("ENTRADA".equalsIgnoreCase(movimiento.getTipo())) {

            producto.setStock(
                    producto.getStock() + movimiento.getCantidad()
            );

        } else if ("SALIDA".equalsIgnoreCase(movimiento.getTipo())) {

            if (producto.getStock() < movimiento.getCantidad()) {
                throw new RuntimeException("Stock insuficiente");
            }

            producto.setStock(
                    producto.getStock() - movimiento.getCantidad()
            );
        }

        productoRepository.save(producto);

        return movimientoInventarioRepository.save(movimiento);
    }

    public MovimientoInventario buscarPorId(Long id) {
        return movimientoInventarioRepository.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        movimientoInventarioRepository.deleteById(id);
    }
}