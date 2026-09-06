package bvl.export;

import bvl.domain.Item;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vuelca cotizaciones a los XLS que consulta el usuario final.
 *
 * <p>Hay dos ficheros por lectura y un exportador para cada uno: el <b>diario</b>, con una hoja por
 * hora de sondeo, y el <b>mensual</b>, con una hoja por dia. Se abre uno
 * ({@link #abrirHojaDiaria} o {@link #abrirHojaMensual}) y luego se escribe con
 * {@link #escribirDatos}; la hoja llega ya con su cabecera y el cursor en la primera fila de datos.
 *
 * <p>No es thread-safe: lleva un cursor abierto sobre un libro. Da igual porque el planificador
 * sondea con un solo hilo, y a proposito, para que dos sondeos no escriban el mismo fichero a la
 * vez.
 */
public class BvlExporter {

    private static final Logger logger = LoggerFactory.getLogger(BvlExporter.class);

    private final RutaXls rutas;

    private XlsWriter libro;

    public BvlExporter(String xlsPath) {
        this.rutas = new RutaXls(xlsPath);
    }

    /** Abre {@code <raiz>/<anio>/<Mes>/<aaaa.mm.dd>.xls} en la hoja de esta hora. */
    public void abrirHojaDiaria(LocalDateTime fecha) {
        abrir(rutas.ficheroDiario(fecha), rutas.hojaDiaria(fecha), fecha);
    }

    /** Abre {@code <raiz>/<anio>/<aaaa.mm_Mes>.xls} en la hoja de este dia. */
    public void abrirHojaMensual(LocalDateTime fecha) {
        abrir(rutas.ficheroMensual(fecha), rutas.hojaMensual(fecha), fecha);
    }

    private void abrir(File fichero, String hoja, LocalDateTime fecha) {
        crearSiNoExiste(fichero);
        libro = new XlsWriter(fichero);
        CabeceraCotizaciones.escribir(libro, hoja, fecha);
    }

    /** Un fichero nuevo nace como copia de la plantilla; uno existente se conserva y se amplia. */
    private static void crearSiNoExiste(File fichero) {
        File carpeta = fichero.getParentFile();
        if (carpeta != null && carpeta.mkdirs()) {
            logger.info("Se creo el directorio: {}", carpeta.getPath());
        }
        logger.info("Escribiendo en {}", fichero.getAbsolutePath());
        try {
            if (fichero.createNewFile()) {
                logger.info("Se creo el archivo: {}", fichero.getName());
                PlantillaXls.copiarSobre(fichero);
            }
        } catch (IOException e) {
            logger.error("No se pudo crear {}: {}", fichero, e.getMessage(), e);
        }
    }

    /**
     * Escribe una fila por cotizacion y vuelca el libro a disco.
     *
     * <p>El orden de las columnas lo manda {@link ColumnaCotizacion}; aqui solo se anaden en ese
     * mismo orden, sin indices sueltos.
     */
    public void escribirDatos(List<Item> data) {
        for (Item item : data) {
            libro.addCell(item.getAccion().getNemonico());
            libro.addCell(item.getCotizacionAnterior());
            libro.addCell(item.getFechaAnterior());
            libro.addCell(item.getCotizacionApertura());
            libro.addCell(ColumnaCotizacion.SIN_DATO);
            libro.addCell(item.getCotizacionUltima());
            libro.addCell(item.getPropuestaCompra());
            libro.addCell(item.getPropuestaVenta());
            libro.addCell(item.getNumeroAcciones());
            libro.addCell(item.getMontoNegociado());
            libro.addCell(item.getNumeroOperaciones());
            libro.addCell(item.getMoneda().getNombre());
            libro.addCell(item.getAccion().getSector().getNombre());
            libro.addCell(item.getVariacionPorcentual());
            libro.addRow();
        }
        libro.write();
    }

    /** El libro abierto. Publico para que los tests puedan releer lo escrito. */
    public XlsWriter getLibro() {
        return libro;
    }

    /**
     * TRAMPA CONOCIDA: no cierra nada, porque {@link XlsWriter#closeResources()} tampoco. Los
     * {@code Workbook} quedan a expensas del recolector. Se conserva tal cual; ver "Trampas
     * conocidas" en CLAUDE.md.
     */
    public void closeResources() {
        // Intencionadamente vacio.
    }
}
