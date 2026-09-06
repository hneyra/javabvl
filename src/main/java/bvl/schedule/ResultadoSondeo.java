package bvl.schedule;

/**
 * Lo que deja un sondeo completado: lo que se acaba de leer y, si la hay, la lectura anterior con
 * la que compararlo.
 *
 * <p>Viaja del hilo del planificador al EDT a traves de {@link SondeoListener}. Antes las
 * cotizaciones vivian en un campo mutable del orquestador que un hilo escribia y el otro leia sin
 * sincronizacion; pasarlas como valor inmutable elimina esa carrera.
 *
 * <p>Se guarda la lectura anterior y no el {@code ResultadoSondeo} anterior a proposito: encadenar
 * resultados retendria en memoria toda la sesion, lectura tras lectura.
 *
 * @param actual la lectura recien hecha
 * @param previa la lectura del sondeo anterior, o {@code null} en el primer sondeo tras arrancar
 */
public record ResultadoSondeo(Lectura actual, Lectura previa) {

    public boolean hayConQueComparar() {
        // Si la BVL republica el mismo instante no es un movimiento nuevo, es la misma lectura.
        return previa != null && !previa.fecha().equals(actual.fecha());
    }
}
