package cl.duoc.wepay_ms_reportes.dto;

import java.util.List;
import java.util.Map;

/**
 * Todo el panel de reportes de un período.
 * anterior es el período de igual largo inmediatamente antes, para comparar.
 * agrupacion indica si ventasPorTramo va por "hora" o por "dia".
 */
public record ResumenReporteDTO(
        String desde,
        String hasta,
        String agrupacion,
        IndicadoresDTO actual,
        IndicadoresDTO anterior,
        Map<String, Long> pedidosPorEstado,
        List<TopProductoDTO> topProductos,
        List<VentasPorTramoDTO> ventasPorTramo) {
}
