
package cl.duoc.wepay_ms_reportes.service;

import cl.duoc.wepay_ms_reportes.dto.PedidoEventoDTO;
import cl.duoc.wepay_ms_reportes.dto.DetalleEventoDTO;

import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import cl.duoc.wepay_ms_reportes.entity.DetallePedidoReporte;
import cl.duoc.wepay_ms_reportes.entity.EventoProcesadoReporte;

import cl.duoc.wepay_ms_reportes.repository.PedidoReporteRepository;
import cl.duoc.wepay_ms_reportes.repository.EventoProcesadoReporteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoReporteService {

    private final PedidoReporteRepository repository;

    private final EventoProcesadoReporteRepository eventoProcesadoRepository;

    public PedidoReporteService(
            PedidoReporteRepository repository,
            EventoProcesadoReporteRepository eventoProcesadoRepository) {

        this.repository = repository;
        this.eventoProcesadoRepository = eventoProcesadoRepository;
    }

    @Transactional
    public void procesarEvento(PedidoEventoDTO evento) {

        // 1. Validar los datos obligatorios
        if (evento.eventId() == null
                || evento.eventId().isBlank()) {

            throw new IllegalArgumentException(
                    "El evento no contiene eventId");
        }

        if (evento.pedidoId() == null) {
            throw new IllegalArgumentException(
                    "El evento no contiene pedidoId");
        }

        // 2. Evitar procesar eventos duplicados
        if (eventoProcesadoRepository.existsById(
                evento.eventId())) {

            System.out.println(
                    "Evento duplicado ignorado: "
                            + evento.eventId());

            return;
        }

        // 3. Buscar el pedido o crear uno nuevo
        PedidoReporte pedido = repository
                .findById(evento.pedidoId())
                .orElseGet(PedidoReporte::new);

        pedido.setPedidoId(evento.pedidoId());

        if (evento.fechaEvento() == null) {
            throw new IllegalArgumentException(
                    "El evento no contiene fechaEvento");
        }

        boolean eventoAntiguo = pedido.getFechaUltimoEvento() != null
                && evento.fechaEvento().isBefore(
                        pedido.getFechaUltimoEvento());

        if (!eventoAntiguo) {

            if (evento.usuarioOid() != null) {
                pedido.setUsuarioOid(evento.usuarioOid());
            }

            if (evento.total() != null) {
                pedido.setTotal(evento.total());
            }

            if (evento.estado() != null) {
                pedido.setEstado(evento.estado());
            }

            if (evento.fechaCreacion() != null) {
                pedido.setFechaCreacion(evento.fechaCreacion());
            }

            if ("OrderDelivered".equals(evento.tipoEvento())) {
                pedido.setFechaEntrega(evento.fechaEvento());
            }

            pedido.setFechaUltimoEvento(evento.fechaEvento());

        } else {

            System.out.println(
                    "Evento antiguo: no se actualizará el estado del pedido "
                            + evento.pedidoId());
        }

        if (pedido.getFechaCreacion() == null
                && evento.fechaCreacion() != null) {

            pedido.setFechaCreacion(evento.fechaCreacion());
        }

        // 5. Guardar productos solo si aún no existen
        if ("OrderCreated".equals(evento.tipoEvento())
                && evento.detalles() != null
                && pedido.getDetalles().isEmpty()) {

            for (DetalleEventoDTO item : evento.detalles()) {
                // Un producto sin id no se puede contar en los reportes:
                // se omite ese producto en vez de rechazar el evento completo.
                if (item.productoId() == null) {
                    System.out.println(
                            "Producto sin productoId omitido en el pedido "
                                    + evento.pedidoId());
                    continue;
                }

                DetallePedidoReporte detalle = new DetallePedidoReporte();

                detalle.setProductoId(item.productoId());

                detalle.setNombreProducto(
                        item.nombreProducto());

                detalle.setPrecioUnitario(
                        item.precioUnitario());

                detalle.setCantidad(item.cantidad());

                pedido.agregarDetalle(detalle);
            }
        }

        // 6. Guardar el pedido y sus detalles
        repository.save(pedido);

        // 7. Registrar el eventId procesado
        EventoProcesadoReporte eventoProcesado = new EventoProcesadoReporte(
                evento.eventId(),
                evento.pedidoId(),
                evento.tipoEvento());

        eventoProcesadoRepository.saveAndFlush(
                eventoProcesado);

        System.out.println(
                "Evento procesado correctamente: "
                        + evento.eventId());

        System.out.println(
                "Pedido actualizado en reportería: "
                        + evento.pedidoId());
    }
}
