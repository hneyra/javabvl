package bvl.service;

import bvl.config.BvlProperties;
import bvl.domain.Item;
import bvl.export.BvlExporter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Vuelca a XLS lo que ya esta en la base.
 *
 * <p>Por cada dia del rango se agrupan las cotizaciones por instante de lectura y cada grupo va a
 * su hoja del fichero diario. Un fallo escribiendo una hoja se registra y no interrumpe las demas:
 * mas vale exportar de menos que perder el sondeo entero.
 */
@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private final LecturaService lecturas;
    private final String xlsPath;

    public ExportService(LecturaService lecturas, BvlProperties properties) {
        this.lecturas = lecturas;
        this.xlsPath = properties.getXlsPath();
    }

    /**
     * Exporta el rango, ambos extremos segun manda {@link #datesEntre}.
     *
     * <p>TRAMPA CONOCIDA: al fichero <b>mensual</b> solo llega el ultimo grupo horario de cada dia,
     * no todos. Se conserva tal cual; ver "Trampas conocidas" en CLAUDE.md.
     */
    public void exportar(LocalDateTime desde, LocalDateTime hasta) {
        BvlExporter exportadorDiario = new BvlExporter(xlsPath);
        BvlExporter exportadorMensual = new BvlExporter(xlsPath);

        List<LocalDateTime> dias = datesEntre(desde, hasta);
        logger.debug("Exportando para las fechas: {}", dias);

        for (LocalDateTime dia : dias) {
            Map<LocalDateTime, List<Item>> porHora = agruparPorHora(lecturas.getItems(dia, dia));

            Map.Entry<LocalDateTime, List<Item>> ultimoGrupo = null;
            for (Map.Entry<LocalDateTime, List<Item>> grupo : porHora.entrySet()) {
                try {
                    exportadorDiario.abrirHojaDiaria(grupo.getKey());
                    ultimoGrupo = grupo;
                    exportadorDiario.escribirDatos(grupo.getValue());
                } catch (Exception e) {
                    logger.error("Fallo exportando la hoja diaria de {}: {}", grupo.getKey(),
                            e.getMessage(), e);
                }
            }

            if (ultimoGrupo == null) {
                continue;
            }
            try {
                exportadorMensual.abrirHojaMensual(ultimoGrupo.getKey());
                exportadorMensual.escribirDatos(ultimoGrupo.getValue());
            } catch (Exception e) {
                logger.error("Fallo exportando la hoja mensual de {}: {}", ultimoGrupo.getKey(),
                        e.getMessage(), e);
            }
        }

        exportadorDiario.closeResources();
        exportadorMensual.closeResources();
    }

    /** Agrupa por instante de lectura. TreeMap: las hojas salen en orden cronologico. */
    private static Map<LocalDateTime, List<Item>> agruparPorHora(List<Item> items) {
        Map<LocalDateTime, List<Item>> porHora = new TreeMap<>();
        if (items == null) {
            return porHora;
        }
        for (Item item : items) {
            porHora.computeIfAbsent(item.getFechaLectura(), k -> new ArrayList<>()).add(item);
        }
        return porHora;
    }

    /**
     * Dias del rango, conservando la hora de {@code desde}.
     *
     * <p>El bucle agrega antes de comprobar, asi que nunca devuelve vacio y excluye {@code hasta}.
     * Con {@code desde == hasta} —el caso real, un sondeo exporta su propia lectura— sale un unico
     * elemento, que es justo lo que hace falta.
     */
    public List<LocalDateTime> datesEntre(LocalDateTime fecha1, LocalDateTime fecha2) {
        List<LocalDateTime> dias = new ArrayList<>();
        LocalDateTime dia = fecha1;
        do {
            dias.add(dia);
            dia = dia.plusDays(1);
        } while (fecha2.isAfter(dia));
        return dias;
    }
}
