
package cl.duoc.wepay_ms_reportes.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_procesados_reporte")
public class EventoProcesadoReporte {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    private Long pedidoId;

    private String tipoEvento;

    private LocalDateTime fechaProcesamiento;

    public EventoProcesadoReporte() {
    }

    public EventoProcesadoReporte(
            String eventId,
            Long pedidoId,
            String tipoEvento) {

        this.eventId = eventId;
        this.pedidoId = pedidoId;
        this.tipoEvento = tipoEvento;
        this.fechaProcesamiento = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(
            LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }
}
