package bvl.market;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.market.dto.BvlItem;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Traduce el JSON de la BVL al dominio en castellano.
 *
 * <p>Los objetos salen nuevos en cada sondeo: {@code Accion}, {@code Sector} y {@code Moneda} se
 * construyen por item aunque se repitan entre ellos. No se comparten instancias porque nadie las
 * modifica despues; viven lo que dura el ciclo, de la lectura al XLS.
 */
@Component
public class CotizacionMapper {

    /** Todos los items de una lectura comparten instante: es lo que identifica el sondeo. */
    public List<Item> aItems(List<BvlItem> cotizaciones, LocalDateTime fechaLectura) {
        return cotizaciones.stream().map(c -> aItem(c, fechaLectura)).toList();
    }

    public Item aItem(BvlItem bvlItem, LocalDateTime fechaLectura) {
        Sector sector = new Sector();
        sector.setNombre(bvlItem.sectorDescription());

        Accion accion = new Accion();
        accion.setEmpresa(bvlItem.companyName());
        accion.setNemonico(bvlItem.nemonico());
        accion.setSector(sector);

        Moneda moneda = new Moneda();
        moneda.setNombre(bvlItem.currency());

        Item item = new Item();
        item.setAccion(accion);
        // La columna segmento es NOT NULL; la BVL deja el codigo de sector a null a menudo.
        item.setSegmento(bvlItem.sectorCode() == null ? "" : bvlItem.sectorCode());
        item.setMoneda(moneda);
        item.setFechaLectura(fechaLectura);
        item.setCotizacionAnterior(bvlItem.previous());
        item.setFechaAnterior(bvlItem.previousDate());
        item.setCotizacionApertura(bvlItem.opening());
        item.setCotizacionUltima(bvlItem.last());
        item.setVariacionPorcentual(bvlItem.percentageChange());
        item.setPropuestaCompra(bvlItem.buy());
        item.setPropuestaVenta(bvlItem.sell());
        item.setNumeroAcciones(bvlItem.negotiatedQuantity());
        item.setNumeroOperaciones(bvlItem.operationsNumber());
        item.setMontoNegociado(bvlItem.negotiatedAmount());
        return item;
    }
}
