package bvl.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copia {@code /res/template.xls} sobre un fichero recien creado.
 *
 * <p>{@link XlsWriter} abre libros existentes, no los crea, asi que todo XLS nuevo nace como copia
 * de la plantilla. De ahi viene la hoja "Hoja1" vacia que arrastran todos los ficheros generados.
 */
public final class PlantillaXls {

    private static final Logger logger = LoggerFactory.getLogger(PlantillaXls.class);

    private static final String RECURSO = "/res/template." + RutaXls.EXTENSION;

    private PlantillaXls() {
    }

    public static void copiarSobre(File destino) {
        try (InputStream plantilla = PlantillaXls.class.getResourceAsStream(RECURSO);
             OutputStream salida = Files.newOutputStream(destino.toPath())) {
            if (plantilla == null) {
                logger.error("No se encontro la plantilla {} en el classpath", RECURSO);
                return;
            }
            plantilla.transferTo(salida);
        } catch (IOException e) {
            logger.error("Error al copiar la plantilla {}: {}", RECURSO, e.getMessage(), e);
        }
    }
}
