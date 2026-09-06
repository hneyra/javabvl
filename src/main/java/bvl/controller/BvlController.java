package bvl.controller;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BvlController {

    @Autowired
    AccionRepository accionRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    LecturaRepository lecturaRepository;

    @Autowired
    SectorRepository sectorRepository;

    @Autowired
    MonedaRepository monedaRepository;

    @RequestMapping("item")
    Iterable<Item> listItems() {
        return itemRepository.findAll();
    }

    @RequestMapping("moneda")
    Iterable<Moneda> listMonedas() {
        return monedaRepository.findAll();
    }

    @RequestMapping("accion")
    Iterable<Accion> listacciones() {
        return accionRepository.findAll();
    }

    @RequestMapping("lectura")
    Iterable<Lectura> listLecturas() {
        return lecturaRepository.findAll();
    }
}
