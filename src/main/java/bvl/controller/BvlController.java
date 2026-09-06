package bvl.controller;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.repository.AccionRepository;
import bvl.repository.ItemRepository;
import bvl.repository.LecturaRepository;
import bvl.repository.MonedaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Volcado en JSON de las tablas, para inspeccionar la base sin abrir la consola de H2.
 *
 * <p>Solo lectura y sin paginar: es una herramienta de diagnostico, no una API.
 */
@RestController
public class BvlController {

    private final AccionRepository accionRepository;
    private final ItemRepository itemRepository;
    private final LecturaRepository lecturaRepository;
    private final MonedaRepository monedaRepository;

    public BvlController(AccionRepository accionRepository,
                         ItemRepository itemRepository,
                         LecturaRepository lecturaRepository,
                         MonedaRepository monedaRepository) {
        this.accionRepository = accionRepository;
        this.itemRepository = itemRepository;
        this.lecturaRepository = lecturaRepository;
        this.monedaRepository = monedaRepository;
    }

    @GetMapping("item")
    Iterable<Item> listItems() {
        return itemRepository.findAll();
    }

    @GetMapping("moneda")
    Iterable<Moneda> listMonedas() {
        return monedaRepository.findAll();
    }

    @GetMapping("accion")
    Iterable<Accion> listAcciones() {
        return accionRepository.findAll();
    }

    @GetMapping("lectura")
    Iterable<Lectura> listLecturas() {
        return lecturaRepository.findAll();
    }
}
