package bvl.market;

import bvl.domain.Accion;
import bvl.domain.Item;
import bvl.domain.Lectura;
import bvl.domain.Moneda;
import bvl.domain.Sector;
import bvl.market.dto.BvlItem;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Traduce el JSON de la BVL al dominio en castellano.
 *
 * <p>Las entidades salen de aqui <b>transitorias y sin id</b>: {@code Accion}, {@code Sector},
 * {@code Moneda} y {@code Lectura} son objetos nuevos en cada sondeo, aunque representen filas que
 * ya existen en la base. Quien las reconcilia por clave natural antes de persistir es
 * {@code CatalogoService}; por eso los {@code saveIfNotExist*} no pueden basarse en el id.
 */
@Component
public class CotizacionMapper {

    /** Todos los items de una lectura comparten instante: identifica la {@link Lectura}. */
    public List<Item> aItems(List<BvlItem> cotizaciones, LocalDateTime fechaLectura) {
        return cotizaciones.stream().map(c -> aItem(c, fechaLectura)).toList();
    }

    public Item aItem(BvlItem bvlItem, LocalDateTime fechaLectura) {
        Lectura lectura = new Lectura();
        lectura.setFecha(fechaLectura);

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
        item.setLectura(lectura);
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
