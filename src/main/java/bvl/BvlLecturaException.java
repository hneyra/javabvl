package bvl;

/**
 * Falla la lectura de datos de la BVL (red caida, servicio no disponible, respuesta ilegible).
 *
 * <p>Antes {@code BvlReader} tragaba el error y abria un {@code JOptionPane} modal desde dentro del
 * servicio, lo que bloqueaba al que llamara hasta que alguien cerrase el dialogo. Ahora el error
 * sube y quien orquesta decide como avisar sin bloquear.
 */
public class BvlLecturaException extends RuntimeException {

    public BvlLecturaException(String message, Throwable cause) {
        super(message, cause);
    }
}
