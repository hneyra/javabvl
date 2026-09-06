package bvl.market;

import bvl.domain.Item;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Frontera con la BVL: entrega cotizaciones ya traducidas al dominio y traduce cualquier fallo a
 * {@link BvlLecturaException}.
 *
 * <p>Es el unico sitio que envuelve errores. {@link BvlClient} los deja subir crudos y
 * {@link CotizacionMapper} no los produce, asi que quien lea este fichero ve de un vistazo todo lo
 * que puede salir mal en una lectura.
 *
 * <p><b>No abre dialogos.</b> Corre en el hilo del planificador y un {@code JOptionPane} lo dejaria
 * colgado hasta que alguien lo cerrase, bloqueando todos los sondeos siguientes. Avisar es cosa de
 * la capa de UI.
 */
@Service
public class LectorBvl {

    private static final Logger logger = LoggerFactory.getLogger(LectorBvl.class);

    private final BvlClient client;
    private final CotizacionMapper mapper;

    public LectorBvl(BvlClient client, CotizacionMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Lee el mercado completo. La fecha se pide <b>antes</b> que las cotizaciones, igual que
     * siempre: si el endpoint de la hora no responde, el fallo se reporta como fallo de fecha y no
     * se llega a pedir el mercado.
     */
    public List<Item> readData() {
        LocalDateTime fechaLectura = getFecha();
        try {
            List<Item> items = mapper.aItems(client.cotizaciones().content(), fechaLectura);
            logger.debug("Total datos leidos: {}", items.size());
            return items;
        } catch (Throwable e) {
            throw new BvlLecturaException("No se pudo leer las cotizaciones de "
                    + client.getUrlCotizaciones() + ": " + e.getMessage(), e);
        }
    }

    /** Instante que identifica la lectura. Lo publica la BVL; no lo pone el reloj local. */
    public LocalDateTime getFecha() {
        try {
            LocalDateTime fecha = client.montoNegociadoDiario().updatedDate();
            logger.debug("Fecha de lectura: {}", fecha);
            return fecha;
        } catch (Throwable e) {
            throw new BvlLecturaException("No se pudo leer la fecha de lectura de "
                    + client.getUrlHora() + ": " + e.getMessage(), e);
        }
    }
}
