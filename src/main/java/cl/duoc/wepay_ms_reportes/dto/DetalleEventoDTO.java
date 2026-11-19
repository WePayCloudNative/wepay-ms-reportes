
package cl.duoc.wepay_ms_reportes.dto;

public record DetalleEventoDTO(
        Long productoId,
        String nombreProducto,
        Double precioUnitario,
        Integer cantidad
) {
}
