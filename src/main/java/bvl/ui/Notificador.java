package bvl.ui;

/**
 * Avisos que no bloquean.
 *
 * <p>Los sondeos corren en el hilo del planificador, uno detras de otro sobre un unico hilo: un
 * aviso modal dejaria colgado ese hilo hasta que alguien lo cerrase y se perderian todos los
 * sondeos siguientes. Por eso los avisos van por la bandeja del sistema.
 *
 * <p>La implementacion {@link #NINGUNO} evita tener que comprobar disponibilidad en cada llamada
 * cuando el escritorio no soporta bandeja.
 */
public interface Notificador {

    Notificador NINGUNO = new Notificador() {
        @Override
        public void aviso(String titulo, String mensaje) {
        }

        @Override
        public void error(String titulo, String mensaje) {
        }
    };

    void aviso(String titulo, String mensaje);

    void error(String titulo, String mensaje);

    /** Mantiene coherente la opcion Ocultar/Mostrar del menu con el estado real de la ventana. */
    default void sincronizarVisibilidad(boolean visible) {
    }
}
