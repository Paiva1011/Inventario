package pe.edu.utp.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.inventario.model.MovimientoInventario;
import pe.edu.utp.inventario.repository.MovimientoInventarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public List<MovimientoInventario> listar() {
        return movimientoInventarioRepository.findAll();
    }

    public MovimientoInventario guardar(MovimientoInventario movimiento) {
        return movimientoInventarioRepository.save(movimiento);
    }

    public MovimientoInventario buscarPorId(Long id) {
        return movimientoInventarioRepository.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        movimientoInventarioRepository.deleteById(id);
    }
}