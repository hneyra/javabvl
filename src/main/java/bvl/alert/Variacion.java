package bvl.alert;

/**
 * Una accion que se movio mas que el umbral configurado.
 *
 * @param nemonico   codigo de la accion en la BVL
 * @param porcentaje variacion publicada, con signo
 */
public record Variacion(String nemonico, double porcentaje) {

    /**
     * Una variacion de exactamente 0 cuenta como bajada: la condicion es {@code > 0}, no
     * {@code >= 0}. Solo se nota con umbral 0, que reporta todo el mercado.
     */
    public boolean esSubida() {
        return porcentaje > 0;
    }
}
