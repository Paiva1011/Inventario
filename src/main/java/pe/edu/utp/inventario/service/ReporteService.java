package pe.edu.utp.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.inventario.dto.StockBajoResponse;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.repository.MovimientoInventarioRepository;
import pe.edu.utp.inventario.repository.ProductoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public List<StockBajoResponse> obtenerProductosStockBajo() {

    return productoRepository.findByStockLessThan(10)
            .stream()
            .map(producto -> new StockBajoResponse(
                    producto.getId(),
                    producto.getNombre(),
                    producto.getStock(),
                    "STOCK BAJO",
                    "Reabastecer producto inmediatamente"
            ))
            .toList();
}

    public long obtenerTotalProductos() {
        return productoRepository.count();
    }

    public List<Producto> obtenerInventario() {
        return productoRepository.findAll();
    }

    public List<MovimientoInventario> obtenerMovimientos() {
        return movimientoRepository.findAll();
    }
}