package cl.duoc.wepay_ms_reportes.dto;

/**
 * Ventas confirmadas en un tramo (una hora o un día, según el largo del período).
 * inicio va en UTC sin zona, igual que el resto de las fechas del sistema.
 */
public record VentasPorTramoDTO(
        String inicio,
        long pedidos,
        double ventas) {
}
