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
 * consulta el usuario. Lo unico que se retiene es la <b>lectura anterior</b>, en memoria, para poder
 * medir cuanto se ha movido cada accion desde el sondeo previo. Al reiniciar la aplicacion se pierde,
 * y el primer sondeo simplemente no tiene con que comparar.
 */
@Service
public class CicloSondeo {

    private static final Logger logger = LoggerFactory.getLogger(CicloSondeo.class);

    private final LectorBvl lector;
    private final ExportService exportacion;

    /**
     * La lectura del ciclo anterior, para poder medir el movimiento intradia. Solo la toca el hilo
     * del planificador, que sondea en serie; {@code volatile} por si algun dia deja de ser asi.
     */
    private volatile Lectura anterior;

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

        Lectura actual = new Lectura(items, fecha);
        exportacion.exportar(actual);

        ResultadoSondeo resultado = new ResultadoSondeo(actual, anterior);
        anterior = actual;
        return resultado;
    }
}
