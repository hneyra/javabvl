package bvl.schedule;

import bvl.domain.Item;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Una lectura completa del mercado: todas las cotizaciones publicadas en un mismo instante.
 *
 * @param items cotizaciones, en el orden en que las publico la BVL
 * @param fecha instante que identifica la lectura, publicado por la BVL
 */
public record Lectura(List<Item> items, LocalDateTime fecha) {
}
