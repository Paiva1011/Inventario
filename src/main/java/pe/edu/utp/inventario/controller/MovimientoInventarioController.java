package pe.edu.utp.inventario.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.service.MovimientoInventarioService;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoInventarioService;

    @GetMapping
    public List<MovimientoInventario> listar() {
        return movimientoInventarioService.listar();
    }

    @GetMapping("/{id}")
    public MovimientoInventario buscarPorId(@PathVariable Long id) {
        return movimientoInventarioService.buscarPorId(id);
    }

    @PostMapping
    public MovimientoInventario guardar(
            @RequestBody MovimientoInventario movimiento) {

        return movimientoInventarioService.guardar(movimiento);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        movimientoInventarioService.eliminar(id);
    }
}
