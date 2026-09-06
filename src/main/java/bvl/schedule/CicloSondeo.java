package bvl.schedule;

import bvl.domain.Item;
import bvl.market.LectorBvl;
import bvl.service.ExportService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Un ciclo completo de sondeo: leer la BVL, guardar y exportar.
 *
 * <p>Es el unico sitio donde se ve el recorrido entero de una lectura. No sabe nada de horarios (de
 * eso va {@link BvlScheduler}) ni de como se presenta el resultado (de eso va la UI a traves de
 * {@link SondeoListener}).
 *
 * <p>Las cotizaciones no se guardan en ninguna base: van de la BVL al XLS, que es el archivo que
 * consulta el usuario.
 */
@Service
public class CicloSondeo {

    private static final Logger logger = LoggerFactory.getLogger(CicloSondeo.class);

    private final LectorBvl lector;
    private final ExportService exportacion;

    public CicloSondeo(LectorBvl lector, ExportService exportacion) {
        this.lector = lector;
        this.exportacion = exportacion;
    }

    /**
     * Ejecuta el ciclo y devuelve lo leido.
     *
     * <p>Propaga cualquier fallo: quien decide que hacer con el es {@link BvlScheduler}, que no
     * puede dejarlo escapar al planificador.
     */
    public ResultadoSondeo process() {
        List<Item> items = lector.readData();
        LocalDateTime fecha = lector.getFecha();
        logger.debug("Leidas {} cotizaciones de las {}", items.size(), fecha);

        ResultadoSondeo resultado = new ResultadoSondeo(items, fecha);
        exportacion.exportar(resultado);
        return resultado;
    }
}
