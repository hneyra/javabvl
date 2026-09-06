package bvl.market.dto;

import java.util.List;

/**
 * Respuesta completa de {@code /v1/stock-quote/market}: el resumen de subidas y bajadas mas la
 * lista de cotizaciones.
 */
public record StockMarket(Integer up, Integer down, Integer equal, List<BvlItem> content) {

    /**
     * Resumen corto a proposito: esto se registra en el log en cada sondeo y el toString por
     * defecto de un record volcaria las cientos de cotizaciones enteras.
     */
    @Override
    public String toString() {
        return "StockMarket{up=" + up + ", down=" + down + ", equal=" + equal
                + ", content#size=" + (content == null ? 0 : content.size()) + '}';
    }
}
