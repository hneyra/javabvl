package bvl.service;

import bvl.domain.Accion;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.repository.AccionRepository;
import bvl.repository.LecturaRepository;
import bvl.repository.MonedaRepository;
import bvl.repository.SectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconcilia por clave natural las entidades que se repiten en cada sondeo.
 *
 * <p>{@code CotizacionMapper} entrega {@code Accion}, {@code Sector}, {@code Moneda} y
 * {@code Lectura} recien construidas y <b>sin id</b>, aunque casi siempre correspondan a filas que
 * ya existen. Persistirlas tal cual crearia un duplicado por ciclo, asi que la busqueda va por lo
 * unico estable que traen: el nemonico, el nombre o la fecha.
 *
 * <p>Devuelven siempre la instancia <b>gestionada</b>: quien las llama tiene que quedarse con el
 * retorno, no con lo que paso como argumento.
 */
@Service
public class CatalogoService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogoService.class);

    private final AccionRepository accionRepository;
    private final LecturaRepository lecturaRepository;
    private final SectorRepository sectorRepository;
    private final MonedaRepository monedaRepository;

    public CatalogoService(AccionRepository accionRepository,
                           LecturaRepository lecturaRepository,
                           SectorRepository sectorRepository,
                           MonedaRepository monedaRepository) {
        this.accionRepository = accionRepository;
        this.lecturaRepository = lecturaRepository;
        this.sectorRepository = sectorRepository;
        this.monedaRepository = monedaRepository;
    }

    @Transactional
    public Lectura saveIfNotExistLectura(Lectura lectura) {
        logger.debug("Lectura a guardar: {}", lectura.getFecha());
        Lectura existente = lecturaRepository.findByFecha(lectura.getFecha());
        if (existente != null) {
            return existente;
        }
        lecturaRepository.save(lectura);
        return lectura;
    }

    @Transactional
    public Moneda saveIfNotExistMoneda(Moneda moneda) {
        Moneda existente = monedaRepository.findByNombre(moneda.getNombre());
        if (existente != null) {
            return existente;
        }
        monedaRepository.save(moneda);
        return moneda;
    }

    @Transactional
    public Sector saveIfNotExistSector(Sector sector) {
        Sector existente = sectorRepository.findByNombre(sector.getNombre());
        if (existente != null) {
            return existente;
        }
        sectorRepository.save(sector);
        return sector;
    }

    /** El sector va primero: {@code Item} es LAZY con {@code cascade = REFRESH}, no persiste padres. */
    @Transactional
    public Accion saveIfNotExistsAccion(Accion accion) {
        Accion existente = accionRepository.findByNemonico(accion.getNemonico());
        if (existente != null) {
            return existente;
        }
        accion.setSector(saveIfNotExistSector(accion.getSector()));
        accionRepository.save(accion);
        return accion;
    }
}
