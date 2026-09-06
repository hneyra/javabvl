package bvl.export;

/**
 * Orden de las columnas de la hoja de cotizaciones.
 *
 * <p>Existe para que la cabecera y la fila de datos no puedan desalinearse en silencio: si alguien
 * mueve una columna aqui, {@link CabeceraCotizaciones} y {@link BvlExporter#escribirDatos} la ven
 * moverse a la vez. Es el orden que llevan los XLS ya generados, asi que no se toca sin migrar los
 * ficheros historicos.
 */
public enum ColumnaCotizacion {

    NEMONICO(0),
    ANTERIOR(1),
    FECHA_ANTERIOR(2),
    APERTURA(3),
    /** No se calcula nunca: la hoja lleva un relleno literal. */
    EX_DERECHO(4),
    ULTIMA(5),
    COMPRA(6),
    VENTA(7),
    NUMERO_ACCIONES(8),
    MONTO_NEGOCIADO(9),
    NUMERO_OPERACIONES(10),
    MONEDA(11),
    SECTOR(12),
    VARIACION(13);

    /** Lo que se escribe en EX-DER, que la BVL no publica. */
    public static final String SIN_DATO = "-------";

    private final short indice;

    ColumnaCotizacion(int indice) {
        this.indice = (short) indice;
    }

    public short indice() {
        return indice;
    }
}
