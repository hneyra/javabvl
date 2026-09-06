package bvl.alert;

import bvl.domain.Item;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Selecciona las acciones cuya variacion alcanza el umbral.
 *
 * <p>Logica pura, sin Swing ni HTML: antes vivia dentro de un {@code @Service} que ademas
 * construia el marcado de la alerta, y no habia forma de probar el criterio por separado del
 * mensaje.
 */
@Component
public class DetectorVariaciones {

    /**
     * @param umbral variacion minima en porcentaje, en valor absoluto. <b>Inclusivo</b>: una accion
     *               que se mueve exactamente el umbral se reporta.
     * @return las variaciones en el mismo orden en que la BVL publico las acciones
     */
    public List<Variacion> detectar(List<Item> items, double umbral) {
        List<Variacion> variaciones = new ArrayList<>();
        for (Item item : items) {
            Double delta = item.getVariacionPorcentual();
            // La BVL no publica variacion de los instrumentos que no se han negociado hoy.
            if (delta == null) {
                continue;
            }
            if (Math.abs(delta) >= umbral) {
                variaciones.add(new Variacion(item.getAccion().getNemonico(), delta));
            }
        }
        return variaciones;
    }
}
