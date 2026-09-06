package bvl.export;

import static bvl.export.ColumnaCotizacion.ANTERIOR;
import static bvl.export.ColumnaCotizacion.APERTURA;
import static bvl.export.ColumnaCotizacion.COMPRA;
import static bvl.export.ColumnaCotizacion.EX_DERECHO;
import static bvl.export.ColumnaCotizacion.FECHA_ANTERIOR;
import static bvl.export.ColumnaCotizacion.MONEDA;
import static bvl.export.ColumnaCotizacion.MONTO_NEGOCIADO;
import static bvl.export.ColumnaCotizacion.NEMONICO;
import static bvl.export.ColumnaCotizacion.NUMERO_ACCIONES;
import static bvl.export.ColumnaCotizacion.NUMERO_OPERACIONES;
import static bvl.export.ColumnaCotizacion.SECTOR;
import static bvl.export.ColumnaCotizacion.ULTIMA;
import static bvl.export.ColumnaCotizacion.VARIACION;
import static bvl.export.ColumnaCotizacion.VENTA;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Las tres filas de cabecera de cada hoja de cotizaciones.
 *
 * <pre>
 *   fila 0   2024-01-15 10:30:00
 *   fila 1   ACCIONES | PRECIOS ................ | PROPUESTAS | No.ACC | MONTO | ...
 *   fila 2            | ANT A.FEC APE EX-DER ULT | COM   VEN  |
 *   fila 3   primera fila de datos
 * </pre>
 *
 * <p>Antes esto eran noventa lineas de llamadas encadenadas a POI donde no se veia el resultado.
 * Ahora la maqueta es la lista de {@link Bloque} de abajo y el codigo que la pinta cabe de un
 * vistazo.
 */
final class CabeceraCotizaciones {

    private static final int FILA_GRUPOS = 1;
    private static final int FILA_SUBTITULOS = 2;

    private static final short GRIS_OSCURO = HSSFColor.HSSFColorPredefined.GREY_80_PERCENT.getIndex();
    private static final short GRIS_MEDIO = HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex();
    private static final short GRIS_CLARO = HSSFColor.HSSFColorPredefined.GREY_40_PERCENT.getIndex();
    private static final short NARANJA = HSSFColor.HSSFColorPredefined.LIGHT_ORANGE.getIndex();
    private static final short BLANCO = HSSFColor.HSSFColorPredefined.WHITE.getIndex();

    /**
     * Un titulo de la cabecera.
     *
     * @param ancho       ancho de columna, o {@code null} para no tocarlo
     * @param colorFondo  abre un estilo nuevo con ese fondo, o {@code null} para reutilizar el del
     *                    bloque anterior: asi los titulos de un mismo grupo comparten formato
     * @param fusion      region a fusionar, o {@code null}
     * @param recuadrar   si la fusion lleva recuadro blanco alrededor
     */
    private record Bloque(String titulo, short columna, Short ancho, Short colorFondo,
                          CellRangeAddress fusion, boolean recuadrar) {

        static Bloque de(String titulo, ColumnaCotizacion columna, Integer ancho, Short color,
                         CellRangeAddress fusion, boolean recuadrar) {
            return new Bloque(titulo, columna.indice(),
                    ancho == null ? null : ancho.shortValue(), color, fusion, recuadrar);
        }
    }

    private static CellRangeAddress fila(int f, ColumnaCotizacion desde, ColumnaCotizacion hasta) {
        return new CellRangeAddress(f, f, desde.indice(), hasta.indice());
    }

    private static CellRangeAddress ambasFilas(ColumnaCotizacion columna) {
        return new CellRangeAddress(FILA_GRUPOS, FILA_SUBTITULOS, columna.indice(), columna.indice());
    }

    /** Fila 1: los grupos. ACCIONES ocupa las dos filas; PRECIOS y PROPUESTAS solo la suya. */
    private static final List<Bloque> GRUPOS = List.of(
            Bloque.de("ACCIONES", NEMONICO, 4000, GRIS_OSCURO, ambasFilas(NEMONICO), false),
            Bloque.de("PRECIOS", ANTERIOR, 2000, GRIS_MEDIO,
                    fila(FILA_GRUPOS, ANTERIOR, ULTIMA), true),
            Bloque.de("PROPUESTAS", COMPRA, null, null,
                    fila(FILA_GRUPOS, COMPRA, VENTA), true),
            Bloque.de("No.ACC o GRUPOS", NUMERO_ACCIONES, 4000, GRIS_CLARO,
                    ambasFilas(NUMERO_ACCIONES), true),
            Bloque.de("MONTO NEGOCIADO", MONTO_NEGOCIADO, 4500, null,
                    ambasFilas(MONTO_NEGOCIADO), true),
            Bloque.de("No. OPE.", NUMERO_OPERACIONES, 2000, null,
                    ambasFilas(NUMERO_OPERACIONES), true),
            // Los titulos van partidos para que quepan en columnas estrechas con ajuste de texto.
            Bloque.de("MON EDA", MONEDA, 2000, null, ambasFilas(MONEDA), true),
            Bloque.de("SEC TOR", SECTOR, 2000, null, ambasFilas(SECTOR), true),
            Bloque.de("VARIA CION", VARIACION, 2000, null, ambasFilas(VARIACION), true));

    /** Fila 2: el detalle de PRECIOS y PROPUESTAS. Bajo ACCIONES no hay subtitulo. */
    private static final List<Bloque> SUBTITULOS = List.of(
            Bloque.de("ANT", ANTERIOR, null, NARANJA, null, false),
            Bloque.de("A.FEC", FECHA_ANTERIOR, 2800, null, null, false),
            Bloque.de("APE", APERTURA, null, null, null, false),
            Bloque.de("EX-DER", EX_DERECHO, null, null, null, false),
            Bloque.de("ULT", ULTIMA, null, null, null, false),
            Bloque.de("COM", COMPRA, null, null, null, false),
            Bloque.de("VEN", VENTA, null, null, null, false));

    private CabeceraCotizaciones() {
    }

    /**
     * Crea la hoja y deja el cursor en la primera fila de datos.
     *
     * <p>Si la hoja ya existia se reemplaza: una lectura repetida a la misma hora sobrescribe, no
     * duplica.
     */
    static void escribir(XlsWriter libro, String nombreHoja, LocalDateTime fecha) {
        libro.createSheet(nombreHoja);
        libro.setCurrentSheet(nombreHoja);

        libro.addRow();
        libro.addCell(RutaXls.marcaDeTiempo(fecha));

        libro.addRow();
        pintar(libro, GRUPOS);

        libro.addRow();
        pintar(libro, SUBTITULOS);

        libro.addRow();
    }

    private static void pintar(XlsWriter libro, List<Bloque> bloques) {
        for (Bloque bloque : bloques) {
            libro.addCell(bloque.titulo(), bloque.columna());
            if (bloque.ancho() != null) {
                libro.setColumnWidth(bloque.ancho());
            }
            if (bloque.colorFondo() != null) {
                libro.nuevoEstilo();
                libro.conCentradoYAjuste();
                libro.conLetraDeCabecera();
                libro.conFondoYBordes(bloque.colorFondo());
            }
            libro.aplicarEstilo();
            if (bloque.fusion() != null) {
                libro.mergeCell(bloque.fusion());
                if (bloque.recuadrar()) {
                    libro.bordearFusion(BLANCO);
                }
            }
        }
    }
}
