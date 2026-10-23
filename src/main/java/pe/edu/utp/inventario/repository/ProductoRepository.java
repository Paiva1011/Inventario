package pe.edu.utp.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.inventario.model.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByStockLessThan(Integer stock);

}

//sayely 