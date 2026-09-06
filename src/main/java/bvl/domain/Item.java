package bvl.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La cotizacion de una {@link Accion} en un instante concreto.
 *
 * <p>{@code fechaLectura} es el instante que publica la BVL y que comparten todos los items de un
 * mismo sondeo: es lo que agrupa una lectura completa y da nombre a la hoja del XLS.
 */
public class Item {

    private Accion accion;

    private Moneda moneda;

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
