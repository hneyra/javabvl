package bvl.market;

import bvl.config.BvlProperties;
import bvl.market.dto.Daily;
import bvl.market.dto.StockMarket;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Transporte HTTP contra dataondemand.bvl.com.pe. Nada mas.
 *
 * <p>No traduce al dominio (de eso va {@link CotizacionMapper}) ni envuelve errores (de eso va
 * {@link LectorBvl}): deja subir la excepcion de WebClient tal cual, para que el traductor de
 * errores este en un unico sitio.
 *
 * <p>Las llamadas son bloqueantes ({@code .block()}) porque quien las consume es el hilo del
 * planificador, que sondea en serie.
 */
@Component
public class BvlClient {

    private static final Logger logger = LoggerFactory.getLogger(BvlClient.class);

    /** Cuerpo que pide el mercado completo: sin filtro de sector ni de empresa, solo el dia. */
    private static final Map<String, Object> TODO_EL_MERCADO_DE_HOY =
            Map.of("sector", "", "isToday", Boolean.TRUE, "companyCode", "", "inputCompany", "");

    private final WebClient webClient;
    private final String urlCotizaciones;
    private final String urlHora;

    public BvlClient(WebClient webClient, BvlProperties properties) {
        this.webClient = webClient;
        this.urlCotizaciones = properties.getUrlCotizaciones();
        this.urlHora = properties.getUrlHora();
    }

    public String getUrlCotizaciones() {
        return urlCotizaciones;
    }

    public String getUrlHora() {
        return urlHora;
    }

    /** POST al endpoint de mercado: todas las cotizaciones publicadas hoy. */
    public StockMarket cotizaciones() {
        StockMarket respuesta = webClient.post().uri(urlCotizaciones)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(TODO_EL_MERCADO_DE_HOY), Map.class)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<StockMarket>() {
                })
                .block();
        logger.info("Respuesta recibida de {}: {}", urlCotizaciones, respuesta);
        return respuesta;
    }

    /** GET al endpoint de monto negociado diario, del que solo interesa la fecha de publicacion. */
    public Daily montoNegociadoDiario() {
        return webClient.get().uri(urlHora).retrieve().bodyToMono(Daily.class).block();
    }
}
