
package cl.duoc.wepay_ms_reportes.dto;

import java.util.Map;

public record KpiResponseDTO(
        long totalPedidos,
        long pedidosEntregados,
        long pedidosActivos,
        double ventasTotales,
        double leadTimePromedioMinutos,
        Map<String, Long> pedidosPorEstado
) {
}
