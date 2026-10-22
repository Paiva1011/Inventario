package pe.edu.utp.inventario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.utp.inventario.model.Categoria;
import pe.edu.utp.inventario.repository.CategoriaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Categoria guardar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    public Categoria actualizar(Long id, Categoria categoria) {

        Categoria categoriaExistente =
                categoriaRepository.findById(id).orElse(null);

        if (categoriaExistente == null) {
            return null;
        }

        categoriaExistente.setNombre(categoria.getNombre());
        categoriaExistente.setDescripcion(categoria.getDescripcion());

        return categoriaRepository.save(categoriaExistente);
    }

    public void eliminar(Long id) {
        categoriaRepository.deleteById(id);
    }
}