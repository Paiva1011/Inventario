package pe.edu.utp.inventario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.model.Producto;
import pe.edu.utp.inventario.service.ReporteService;
import pe.edu.utp.inventario.dto.StockBajoResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/stock-bajo")
    public List<StockBajoResponse> stockBajo() {
    return reporteService.obtenerProductosStockBajo();
    }

    @GetMapping("/total-productos")
    public Map<String, Long> totalProductos() {
        return Map.of(
                "total",
                reporteService.obtenerTotalProductos()
        );
    }

    @GetMapping("/inventario")
    public List<Producto> inventario() {
        return reporteService.obtenerInventario();
    }

    @GetMapping("/movimientos")
    public List<MovimientoInventario> movimientos() {
        return reporteService.obtenerMovimientos();
    }
    
}