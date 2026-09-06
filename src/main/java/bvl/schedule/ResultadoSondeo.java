package bvl.schedule;

import bvl.domain.Item;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que deja un sondeo completado.
 *
 * <p>Viaja del hilo del planificador al EDT a traves de {@link SondeoListener}. Antes las
 * cotizaciones vivian en un campo mutable del orquestador que un hilo escribia y el otro leia sin
 * sincronizacion; pasarlas como valor inmutable elimina esa carrera y, de paso, la segunda peticion
 * HTTP que hacia la ventana solo para poner la fecha en el titulo del aviso.
 *
 * @param items cotizaciones leidas, en el orden en que las publico la BVL
 * @param fecha instante que identifica la lectura, publicado por la BVL
 */
public record ResultadoSondeo(List<Item> items, LocalDateTime fecha) {
}
