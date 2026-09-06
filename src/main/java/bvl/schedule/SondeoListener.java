package bvl.schedule;

/**
 * Avisos de cada disparo del sondeo.
 *
 * <p>Existe para que {@link BvlScheduler} no sepa nada de Swing: la ventana se registra como
 * listener y decide como presentar el resultado. Las implementaciones no deben bloquear, porque
 * corren en el hilo del planificador.
 */
public interface SondeoListener {

    void onSondeoCompletado();

    void onSondeoFallido(Throwable error);
}
