
package cl.duoc.wepay_ms_reportes.consumer;

import cl.duoc.wepay_ms_reportes.dto.PedidoEventoDTO;
import cl.duoc.wepay_ms_reportes.service.PedidoReporteService;

import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoEventoConsumer {

    private final ObjectMapper objectMapper;
    private final PedidoReporteService pedidoReporteService;

    public PedidoEventoConsumer(
            ObjectMapper objectMapper,
            PedidoReporteService pedidoReporteService) {

        this.objectMapper = objectMapper;
        this.pedidoReporteService = pedidoReporteService;
    }

    @KafkaListener(
        topics = "orders.events",
        groupId = "wepay-reportes-group"
    )
    public void recibirEvento(String mensaje) {

        // Evitar procesar mensajes vacíos
        if (mensaje == null || mensaje.isBlank()) {
            System.out.println(
                "Kafka: mensaje vacío ignorado"
            );
            return;
        }

        try {

            PedidoEventoDTO evento = objectMapper.readValue(
                mensaje,
                PedidoEventoDTO.class
            );

            System.out.println(
                "=== EVENTO KAFKA RECIBIDO ==="
            );

            System.out.println(
                "Tipo: " + evento.tipoEvento()
            );

            System.out.println(
                "Pedido: " + evento.pedidoId()
            );

            System.out.println(
                "Estado: " + evento.estado()
            );

            pedidoReporteService.procesarEvento(evento);

            System.out.println(
                "Evento procesado correctamente"
            );

        } catch (Exception e) {

            System.err.println(
                "Error procesando evento: " + e.getMessage()
            );

            throw new IllegalStateException(
                "Error procesando evento Kafka", e
            );
        }
    }
}
