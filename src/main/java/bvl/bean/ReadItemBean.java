package bvl.bean;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Deprecated
public class ReadItemBean {

    private Long cantidadNegociada;
    private Double compra;
    private Double venta;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaAnterior;
    private String nemonico;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fechaUltima;
    private Double exderecho;
    private Long montoNegociado;
    private Double apertura;
    private Double anterior;
    private String nombreCorto;
    private Double ultima;
    private Double variacionPorcentual;
    private String codigoEmpresa;
    private String sectorDescripcion;
    private Double minima;
    private Integer unidad;
    private String razonSocial;
    private String segmento;
    private String codigoSector;
    private Double maxima;
    private String moneda;
    private Long numeroOperaciones;

    public Long getCantidadNegociada() {
        return cantidadNegociada;
    }

    public void setCantidadNegociada(Long cantidadNegociada) {
        this.cantidadNegociada = cantidadNegociada;
    }

    public Double getCompra() {
        return compra;
    }

    public void setCompra(Double compra) {
        this.compra = compra;
    }

    public Double getVenta() {
        return venta;
    }

    public void setVenta(Double venta) {
        this.venta = venta;
    }

    public LocalDate getFechaAnterior() {
        return fechaAnterior;
    }

    public void setFechaAnterior(LocalDate fechaAnterior) {
        this.fechaAnterior = fechaAnterior;
    }

    public String getNemonico() {
        return nemonico;
    }

    public void setNemonico(String nemonico) {
        this.nemonico = nemonico;
    }

    public LocalDateTime getFechaUltima() {
        return fechaUltima;
    }

    public void setFechaUltima(LocalDateTime fechaUltima) {
        this.fechaUltima = fechaUltima;
    }

    public Double getExderecho() {
        return exderecho;
    }

    public void setExderecho(Double exderecho) {
        this.exderecho = exderecho;
    }

    public Long getMontoNegociado() {
        return montoNegociado;
    }

    public void setMontoNegociado(Long montoNegociado) {
        this.montoNegociado = montoNegociado;
    }

    public Double getApertura() {
        return apertura;
    }

    public void setApertura(Double apertura) {
        this.apertura = apertura;
    }

    public Double getAnterior() {
        return anterior;
    }

    public void setAnterior(Double anterior) {
        this.anterior = anterior;
    }

    public String getNombreCorto() {
        return nombreCorto;
    }

    public void setNombreCorto(String nombreCorto) {
        this.nombreCorto = nombreCorto;
    }

    public Double getUltima() {
        return ultima;
    }

    public void setUltima(Double ultima) {
        this.ultima = ultima;
    }

    public Double getVariacionPorcentual() {
        return variacionPorcentual;
    }

    public void setVariacionPorcentual(Double variacionPorcentual) {
        this.variacionPorcentual = variacionPorcentual;
    }

    public String getCodigoEmpresa() {
        return codigoEmpresa;
    }

    public void setCodigoEmpresa(String codigoEmpresa) {
        this.codigoEmpresa = codigoEmpresa;
    }

    public String getSectorDescripcion() {
        return sectorDescripcion;
    }

    public void setSectorDescripcion(String sectorDescripcion) {
        this.sectorDescripcion = sectorDescripcion;
    }

    public Double getMinima() {
        return minima;
    }

    public void setMinima(Double minima) {
        this.minima = minima;
    }

    public Integer getUnidad() {
        return unidad;
    }

    public void setUnidad(Integer unidad) {
        this.unidad = unidad;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getSegmento() {
        return segmento;
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public String getCodigoSector() {
        return codigoSector;
    }

    public void setCodigoSector(String codigoSector) {
        this.codigoSector = codigoSector;
    }

    public Double getMaxima() {
        return maxima;
    }

    public void setMaxima(Double maxima) {
        this.maxima = maxima;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public Long getNumeroOperaciones() {
        return numeroOperaciones;
    }

    public void setNumeroOperaciones(Long numeroOperaciones) {
        this.numeroOperaciones = numeroOperaciones;
    }
}
