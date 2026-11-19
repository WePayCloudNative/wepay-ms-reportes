
package cl.duoc.wepay_ms_reportes.service;

import cl.duoc.wepay_ms_reportes.dto.PedidoEventoDTO;
import cl.duoc.wepay_ms_reportes.entity.PedidoReporte;
import cl.duoc.wepay_ms_reportes.repository.PedidoReporteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoReporteService {

    private final PedidoReporteRepository repository;

    public PedidoReporteService(
            PedidoReporteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void procesarEvento(PedidoEventoDTO evento) {

        if (evento.pedidoId() == null) {
            throw new IllegalArgumentException(
                "El evento no contiene pedidoId"
            );
        }

        PedidoReporte pedido = repository
            .findById(evento.pedidoId())
            .orElseGet(PedidoReporte::new);

        pedido.setPedidoId(evento.pedidoId());

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

        if ("OrderDelivered".equals(evento.tipoEvento())
                && evento.fechaEvento() != null) {
            pedido.setFechaEntrega(evento.fechaEvento());
        }

        repository.save(pedido);

        System.out.println(
            "Pedido actualizado en reportería: "
                + evento.pedidoId()
        );
    }
}
