
package cl.duoc.wepay_ms_reportes.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos_reporte")
public class PedidoReporte {

    @Id
    private Long pedidoId;

    private String usuarioOid;

    private Double total;

    private String estado;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaEntrega;

    private LocalDateTime fechaUltimoEvento;

    // Relación con los productos del pedido
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedidoReporte> detalles = new ArrayList<>();

    public PedidoReporte() {
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getUsuarioOid() {
        return usuarioOid;
    }

    public void setUsuarioOid(String usuarioOid) {
        this.usuarioOid = usuarioOid;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    // Obtener los productos asociados al pedido
    public List<DetallePedidoReporte> getDetalles() {
        return detalles;
    }

    // Agregar un producto al pedido
    public void agregarDetalle(DetallePedidoReporte detalle) {
        detalle.setPedido(this);
        this.detalles.add(detalle);
    }

    // Eliminar los detalles anteriores sin duplicarlos
    public void limpiarDetalles() {
        for (DetallePedidoReporte detalle : detalles) {
            detalle.setPedido(null);
        }

        this.detalles.clear();
    }

    public LocalDateTime getFechaUltimoEvento() {
        return fechaUltimoEvento;
    }

    public void setFechaUltimoEvento(LocalDateTime fechaUltimoEvento) {
        this.fechaUltimoEvento = fechaUltimoEvento;
    }

}
