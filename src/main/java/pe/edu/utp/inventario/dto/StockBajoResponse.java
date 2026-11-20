package pe.edu.utp.inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockBajoResponse {

    private Long id;
    private String nombre;
    private Integer stock;
    private String estado;
    private String alerta;
}
