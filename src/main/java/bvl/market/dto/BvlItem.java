package bvl.market.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una fila de la respuesta de {@code /v1/stock-quote/market}, tal cual la publica la BVL.
 *
 * <p>Es el contrato con dataondemand, no el modelo de la aplicacion: por eso conserva los nombres
 * en ingles del JSON, erratas incluidas ({@code minimun}, {@code maximun}). La traduccion al
 * dominio en castellano la hace {@link bvl.market.CotizacionMapper}.
 *
 * <p>Todos los componentes son de tipo objeto porque la BVL omite o deja a null casi cualquier
 * campo de un instrumento que no se ha negociado en el dia.
 */
public record BvlItem(
        String companyCode,
        String companyName,
        String shortName,
        String nemonico,
        String sectorCode,
        String sectorDescription,
        LocalDateTime lastDate,
        Double buy,
        Double sell,
        LocalDate previousDate,
        Double last,
        Double minimun,
        Double maximun,
        Double opening,
        Double previous,
        Long negotiatedQuantity,
        Long negotiatedAmount,
        Long negotiatedNationalAmount,
        Long operationsNumber,
        Double exderecho,
        Double percentageChange,
        String currency,
        Integer unity,
        String segment,
        LocalDateTime createdDate,
        Long numNeg) {
}
