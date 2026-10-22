package pe.edu.utp.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.inventario.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

}