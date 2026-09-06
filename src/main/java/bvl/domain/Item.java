package bvl.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "lectura_id", referencedColumnName = "id", updatable = true, insertable = true, unique = false)
    private Lectura lectura;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "accion_id", referencedColumnName = "id", updatable = true, insertable = true, unique = false)
    private Accion accion;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "moneda_id", referencedColumnName = "id", updatable = true, insertable = true, unique = false)
    private Moneda moneda;

    @Column(insertable = true, updatable = true, unique = false, nullable = false, length = 16)
    private String segmento;

    private Double cotizacionAnterior;

    private LocalDate fechaAnterior;

    private LocalDateTime fechaLectura;

    private Double cotizacionApertura;

    private Double cotizacionUltima;

    private Double variacionPorcentual;

    private Double propuestaCompra;

    private Double propuestaVenta;

    private Long numeroAcciones;

    private Long numeroOperaciones;

    private Long montoNegociado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lectura getLectura() {
        return lectura;
    }

    public void setLectura(Lectura lectura) {
        this.lectura = lectura;
    }

    public Accion getAccion() {
        return accion;
    }

    public void setAccion(Accion accion) {
        this.accion = accion;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public Double getCotizacionAnterior() {
        return cotizacionAnterior;
    }

    public void setCotizacionAnterior(Double cotizacionAnterior) {
        this.cotizacionAnterior = cotizacionAnterior;
    }

    public LocalDate getFechaAnterior() {
        return fechaAnterior;
    }

    public void setFechaAnterior(LocalDate fechaAnterior) {
        this.fechaAnterior = fechaAnterior;
    }

    public LocalDateTime getFechaLectura() {
        return fechaLectura;
    }

    public void setFechaLectura(LocalDateTime fechaLectura) {
        this.fechaLectura = fechaLectura;
    }

    public Double getCotizacionApertura() {
        return cotizacionApertura;
    }

    public void setCotizacionApertura(Double cotizacionApertura) {
        this.cotizacionApertura = cotizacionApertura;
    }

    public Double getCotizacionUltima() {
        return cotizacionUltima;
    }

    public void setCotizacionUltima(Double cotizacionUltima) {
        this.cotizacionUltima = cotizacionUltima;
    }

    public Double getVariacionPorcentual() {
        return variacionPorcentual;
    }

    public void setVariacionPorcentual(Double variacionPorcentual) {
        this.variacionPorcentual = variacionPorcentual;
    }

    public Double getPropuestaCompra() {
        return propuestaCompra;
    }

    public void setPropuestaCompra(Double propuestaCompra) {
        this.propuestaCompra = propuestaCompra;
    }

    public Double getPropuestaVenta() {
        return propuestaVenta;
    }

    public void setPropuestaVenta(Double propuestaVenta) {
        this.propuestaVenta = propuestaVenta;
    }

    public Long getNumeroAcciones() {
        return numeroAcciones;
    }

    public void setNumeroAcciones(Long numeroAcciones) {
        this.numeroAcciones = numeroAcciones;
    }

    public Long getNumeroOperaciones() {
        return numeroOperaciones;
    }

    public void setNumeroOperaciones(Long numeroOperaciones) {
        this.numeroOperaciones = numeroOperaciones;
    }

    public Long getMontoNegociado() {
        return montoNegociado;
    }

    public void setMontoNegociado(Long montoNegociado) {
        this.montoNegociado = montoNegociado;
    }
}
