package bvl.market.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta de {@code /v1/traded-amount/daily}.
 *
 * <p>De todo esto la aplicacion solo usa {@code updatedDate}: es el instante que identifica la
 * lectura, y lo publica la BVL en vez de ponerlo el reloj local.
 */
public record Daily(
        String id,
        LocalDate date,
        Double amountSoles,
        Double amountDollars,
        Long numberOperations,
        Double porcentual,
        LocalDateTime createdDate,
        LocalDateTime updatedDate) {
}
