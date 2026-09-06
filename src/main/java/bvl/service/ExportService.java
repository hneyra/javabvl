package bvl.service;

import bvl.config.BvlProperties;
import bvl.domain.Item;
import bvl.export.BvlExporter;
import bvl.schedule.ResultadoSondeo;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Vuelca una lectura a los dos XLS que consulta el usuario: el del dia, con una hoja por hora de
 * sondeo, y el del mes, con una hoja por dia.
 *
 * <p>Exporta <b>lo que se acaba de leer</b>. Antes escribia las cotizaciones en la base y las
 * releia para exportarlas, un viaje de ida y vuelta que ademas arrastraba los items duplicados de
 * ciclos anteriores a la hoja del dia.
 *
 * <p>Un fallo escribiendo un fichero se registra y no impide el otro: mas vale exportar de menos
 * que perder la lectura entera.
 */
@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private final String xlsPath;

    public ExportService(BvlProperties properties) {
        this.xlsPath = properties.getXlsPath();
    }

    public void exportar(ResultadoSondeo resultado) {
        exportar(resultado.fecha(), resultado.items());
    }

    public void exportar(LocalDateTime fecha, List<Item> items) {
        logger.debug("Exportando {} cotizaciones de las {}", items.size(), fecha);

        BvlExporter diario = new BvlExporter(xlsPath);
        escribir("diaria", fecha, () -> {
            diario.abrirHojaDiaria(fecha);
            diario.escribirDatos(items);
        });

        BvlExporter mensual = new BvlExporter(xlsPath);
        escribir("mensual", fecha, () -> {
            mensual.abrirHojaMensual(fecha);
            mensual.escribirDatos(items);
        });

        diario.closeResources();
        mensual.closeResources();
    }

    private static void escribir(String cual, LocalDateTime fecha, Runnable escritura) {
        try {
            escritura.run();
        } catch (Exception e) {
            logger.error("Fallo exportando la hoja {} de {}: {}", cual, fecha, e.getMessage(), e);
        }
    }
}
