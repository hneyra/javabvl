package bvl.alert;

import bvl.domain.Item;
import bvl.schedule.Lectura;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Selecciona las acciones cuya variacion alcanza el umbral.
 *
 * <p>Dos criterios distintos, que no hay que confundir:
 * <ul>
 *   <li>{@link #detectar} usa la variacion que <b>publica la BVL</b>, que es contra el cierre de la
 *       sesion anterior. Es la alerta de siempre y no necesita recordar nada.
 *   <li>{@link #detectarDesde} mide el movimiento <b>desde el sondeo anterior</b> de la propia
 *       aplicacion. Eso si exige recordar la lectura previa, y por eso el primer sondeo tras
 *       arrancar no la produce.
 * </ul>
 *
 * <p>Logica pura, sin Swing ni HTML: antes vivia dentro de un {@code @Service} que ademas
 * construia el marcado de la alerta, y no habia forma de probar el criterio por separado del
 * mensaje.
 */
@Component
public class DetectorVariaciones {

    /**
     * Variacion contra el cierre de la sesion anterior, tal y como la publica la BVL.
     *
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

    /**
     * Movimiento de cada accion entre dos lecturas de la aplicacion, sobre la ultima cotizacion.
     *
     * <p>Solo entran las acciones presentes en las dos lecturas y con cotizacion en ambas: la BVL
     * deja de publicar precio en cuanto un instrumento no se negocia, y una accion que aparece a
     * media sesion no se ha "movido", simplemente no estaba antes.
     *
     * @param umbral mismo criterio que en {@link #detectar}: porcentaje absoluto, inclusivo
     * @return las variaciones en el orden de la lectura actual
     */
    public List<Variacion> detectarDesde(Lectura previa, Lectura actual, double umbral) {
        Map<String, Double> cotizacionPrevia = new HashMap<>();
        for (Item item : previa.items()) {
            if (item.getCotizacionUltima() != null) {
                cotizacionPrevia.put(item.getAccion().getNemonico(), item.getCotizacionUltima());
            }
        }

        List<Variacion> variaciones = new ArrayList<>();
        for (Item item : actual.items()) {
            Double ahora = item.getCotizacionUltima();
            Double antes = cotizacionPrevia.get(item.getAccion().getNemonico());
            // Un precio anterior de 0 no permite calcular porcentaje: se descarta en vez de
            // producir un infinito que acabaria en pantalla.
            if (ahora == null || antes == null || antes == 0d) {
                continue;
            }
            double delta = (ahora - antes) / antes * 100d;
            if (Math.abs(delta) >= umbral) {
                variaciones.add(new Variacion(item.getAccion().getNemonico(), delta));
            }
        }
        return variaciones;
    }
}
