
package cl.duoc.wepay_ms_reportes.dto;

public record TopProductoDTO(
    Long productoId,
    String nombreProducto,
    Long cantidadVendida,
    Double ingresosTotales
) {
}
