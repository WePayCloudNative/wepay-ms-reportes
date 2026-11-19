
package cl.duoc.wepay_ms_reportes.dto;

public record VentasPorHoraDTO(
    String hora,
    Long cantidadPedidos,
    Double totalVentas
) {
}
