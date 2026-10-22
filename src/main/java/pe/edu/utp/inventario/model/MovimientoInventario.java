package pe.edu.utp.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;

    private Integer cantidad;

    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
}