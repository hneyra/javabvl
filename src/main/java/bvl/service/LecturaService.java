package bvl.service;

import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.repository.ItemRepository;
import bvl.repository.LecturaRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

/**
 * Persistencia y consulta de las lecturas y sus cotizaciones.
 *
 * <p>{@code Lectura} es el eje temporal: una fila por instante de sondeo, y un {@code Item} por
 * accion dentro de cada una.
 */
@Service
public class LecturaService {

    private final ItemRepository itemRepository;
    private final LecturaRepository lecturaRepository;
    private final CatalogoService catalogo;

    public LecturaService(ItemRepository itemRepository,
                          LecturaRepository lecturaRepository,
                          CatalogoService catalogo) {
        this.itemRepository = itemRepository;
        this.lecturaRepository = lecturaRepository;
        this.catalogo = catalogo;
    }

    /**
     * Guarda una lectura completa: primero reconcilia los catalogos, despues inserta los items.
     *
     * <p>Ese orden es obligatorio. Las relaciones de {@code Item} son LAZY con
     * {@code cascade = REFRESH}, asi que no arrastran a sus padres al persistir: si la accion, la
     * moneda o la lectura no estan guardadas antes, la insercion falla.
     *
     * <p>La {@code Lectura} nace de los items, no del parametro {@code fecha}: una lista vacia no
     * crea nada.
     *
     * <p>TRAMPA CONOCIDA: no hay guarda de duplicados. Repetir la misma lectura reinserta todos los
     * items sobre la misma {@code Lectura}. Se conserva tal cual; ver "Trampas conocidas" en
     * CLAUDE.md y {@code saveDataRepetirLaMismaLecturaDuplicaItems}.
     */
    public void saveData(List<Item> data, LocalDateTime fecha) {
        for (Item item : data) {
            item.setAccion(catalogo.saveIfNotExistsAccion(item.getAccion()));
            item.setMoneda(catalogo.saveIfNotExistMoneda(item.getMoneda()));
            item.setLectura(catalogo.saveIfNotExistLectura(item.getLectura()));
        }
        itemRepository.saveAll(data);
    }

    /** Cotizaciones de una lectura concreta, por su instante exacto. */
    public List<Item> getItems(LocalDateTime fecha) {
        return itemRepository.findByLectura(lecturaRepository.findByFecha(fecha));
    }

    /** Cotizaciones de todas las lecturas del rango, ambos extremos incluidos. */
    public List<Item> getItems(LocalDateTime desde, LocalDateTime hasta) {
        return itemRepository.findByLecturaIn(lecturaRepository.findByFechaBetween(desde, hasta));
    }

    /**
     * TRAMPA CONOCIDA: pese al nombre devuelve la lectura <b>mas antigua</b>, no la ultima: ordena
     * {@code ASC} y toma la primera pagina. Se conserva tal cual; ver "Trampas conocidas" en
     * CLAUDE.md y {@code getLastDateDevuelveLaMasAntigua}.
     *
     * @return {@code null} si no hay ninguna lectura guardada
     */
    public LocalDateTime getLastDate() {
        Page<Lectura> lecturas = lecturaRepository
                .findAll(PageRequest.of(0, 1, Direction.ASC, "fecha"));
        List<Lectura> contenido = lecturas.getContent();
        return contenido.isEmpty() ? null : contenido.get(0).getFecha();
    }

    /**
     * TRAMPA CONOCIDA: lanza {@code DateTimeException} para cualquier entrada, porque
     * {@code LocalDateTime.from(LocalDate)} no tiene campos de hora con los que construirse. Esta
     * muerto: nadie lo llama en produccion. Se conserva tal cual; ver "Trampas conocidas" en
     * CLAUDE.md y {@code getHorasSiempreFalla}.
     */
    public Map<Long, LocalDateTime> getHoras(LocalDateTime fecha) {
        LocalDateTime desde = LocalDateTime.from(fecha.toLocalDate());
        LocalDateTime hasta = LocalDateTime.from(desde.plusDays(1));

        Map<Long, LocalDateTime> horas = new HashMap<>();
        for (Lectura lectura : lecturaRepository.findByFechaBetween(desde, hasta)) {
            horas.put(lectura.getId(), lectura.getFecha());
        }
        return horas;
    }
}
