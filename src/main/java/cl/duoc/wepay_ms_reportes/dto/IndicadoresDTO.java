package cl.duoc.wepay_ms_reportes.dto;

/**
 * Indicadores de un período. "Confirmados" son los pedidos con pago aceptado
 * (ACEPTADO, EN_PREPARACION, DESPACHADO o ENTREGADO): ahí se cuenta la venta.
 */
public record IndicadoresDTO(
        long totalPedidos,
        long pedidosConfirmados,
        long pedidosEntregados,
        long pedidosActivos,
        long pedidosCancelados,
        double ventasConfirmadas,
        double ventasEntregadas,
        double montoCancelado,
        double ticketPromedio,
        double tasaCancelacion,
        double leadTimePromedioMinutos) {
}
