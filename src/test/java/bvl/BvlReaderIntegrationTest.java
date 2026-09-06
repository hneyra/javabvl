package bvl;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.domain.Item;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * {@link BvlReader} contra un WebClient de mentira: fija el contrato con la API de la BVL y el
 * mapeo BvlItem -> Item, que es donde se rompe todo si dataondemand cambia el JSON.
 *
 * <p>No se llama a {@code reader.init()} a proposito: ese {@code @PostConstruct} desactiva la
 * validacion de certificados TLS de toda la JVM, y no queremos ese efecto en la suite.
 */
class BvlReaderIntegrationTest {

  private static final String URL_MERCADO = "http://localhost/v1/stock-quote/market";
  private static final String URL_HORA = "http://localhost/v1/traded-amount/daily";

  private static final String JSON_DAILY = """
      {
        "id": "1",
        "date": "2024-01-15",
        "amountSoles": 1234.5,
        "amountDollars": 456.7,
        "numberOperations": 89,
        "porcentual": 1.2,
        "createdDate": "2024-01-15T09:00:00",
        "updatedDate": "2024-01-15T16:30:45"
      }
      """;

  private static final String JSON_MERCADO = """
      {
        "up": 2,
        "down": 1,
        "equal": 0,
        "content": [
          {
            "companyCode": "A1",
            "companyName": "Alicorp S.A.A.",
            "shortName": "ALICORP",
            "nemonico": "ALICORC1",
            "sectorCode": "DIV",
            "sectorDescription": "DIVERSAS",
            "lastDate": "2024-01-15T16:29:00",
            "buy": 7.30,
            "sell": 7.40,
            "previousDate": "2024-01-12",
            "last": 7.35,
            "minimun": 7.20,
            "maximun": 7.45,
            "opening": 7.25,
            "previous": 7.10,
            "negotiatedQuantity": 15000,
            "negotiatedAmount": 110250,
            "operationsNumber": 42,
            "percentageChange": 3.52,
            "currency": "S/",
            "segment": "REGULAR"
          },
          {
            "companyName": "Credicorp Ltd.",
            "nemonico": "BAP",
            "sectorCode": null,
            "sectorDescription": "BANCOS Y FINANCIERAS",
            "previousDate": null,
            "last": null,
            "percentageChange": null,
            "currency": "US$"
          }
        ]
      }
      """;

  private BvlReader reader;

  @BeforeEach
  void setUp() {
    ExchangeFunction stub = request -> {
      String path = request.url().getPath();
      String json = path.endsWith("/daily") ? JSON_DAILY : JSON_MERCADO;
      return Mono.just(ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .body(json)
          .build());
    };

    reader = new BvlReader();
    ReflectionTestUtils.setField(reader, "webClient",
        WebClient.builder().exchangeFunction(stub).build());
    ReflectionTestUtils.setField(reader, "urlCotizaciones", URL_MERCADO);
    ReflectionTestUtils.setField(reader, "urlHora", URL_HORA);
  }

  @Test
  @DisplayName("getFecha toma updatedDate del endpoint de monto negociado diario")
  void getFechaLeeUpdatedDate() {
    // Es el instante que identifica la Lectura completa, no lo pone el reloj local.
    assertThat(reader.getFecha()).isEqualTo(LocalDateTime.of(2024, 1, 15, 16, 30, 45));
  }

  @Test
  @DisplayName("readData mapea cada BvlItem del JSON a un Item")
  void readDataDevuelveUnItemPorAccion() {
    List<Item> items = reader.readData();

    assertThat(items).hasSize(2);
    assertThat(items).extracting(i -> i.getAccion().getNemonico())
        .containsExactly("ALICORC1", "BAP");
  }

  @Test
  @DisplayName("readData traduce todos los campos de cotizacion a su nombre en castellano")
  void readDataMapeaTodosLosCampos() {
    Item item = reader.readData().get(0);

    assertThat(item.getAccion().getNemonico()).isEqualTo("ALICORC1");
    assertThat(item.getAccion().getEmpresa()).isEqualTo("Alicorp S.A.A.");
    assertThat(item.getAccion().getSector().getNombre()).isEqualTo("DIVERSAS");
    assertThat(item.getMoneda().getNombre()).isEqualTo("S/");
    assertThat(item.getSegmento()).isEqualTo("DIV");
    assertThat(item.getCotizacionAnterior()).isEqualTo(7.10);
    assertThat(item.getFechaAnterior()).isEqualTo(LocalDate.of(2024, 1, 12));
    assertThat(item.getCotizacionApertura()).isEqualTo(7.25);
    assertThat(item.getCotizacionUltima()).isEqualTo(7.35);
    assertThat(item.getVariacionPorcentual()).isEqualTo(3.52);
    assertThat(item.getPropuestaCompra()).isEqualTo(7.30);
    assertThat(item.getPropuestaVenta()).isEqualTo(7.40);
    assertThat(item.getNumeroAcciones()).isEqualTo(15000L);
    assertThat(item.getNumeroOperaciones()).isEqualTo(42L);
    assertThat(item.getMontoNegociado()).isEqualTo(110250L);
  }

  @Test
  @DisplayName("todos los items de una llamada comparten el instante de lectura")
  void readDataComparteLaFechaDeLectura() {
    LocalDateTime esperada = LocalDateTime.of(2024, 1, 15, 16, 30, 45);

    List<Item> items = reader.readData();

    assertThat(items).allSatisfy(i -> {
      assertThat(i.getFechaLectura()).isEqualTo(esperada);
      assertThat(i.getLectura().getFecha()).isEqualTo(esperada);
    });
  }

  @Test
  @DisplayName("un sectorCode nulo se convierte en cadena vacia, que es lo que exige la columna")
  void sectorCodeNuloSeConvierteEnCadenaVacia() {
    // segmento es NOT NULL en la tabla item; el reader lo normaliza antes de persistir.
    Item bap = reader.readData().get(1);

    assertThat(bap.getSegmento()).isEmpty();
  }

  @Test
  @DisplayName("los campos ausentes u opcionales del JSON llegan como null, no rompen la lectura")
  void camposAusentesLleganComoNull() {
    // La BVL no publica cotizacion para instrumentos sin negociar en el dia.
    Item bap = reader.readData().get(1);

    assertThat(bap.getCotizacionUltima()).isNull();
    assertThat(bap.getVariacionPorcentual()).isNull();
    assertThat(bap.getFechaAnterior()).isNull();
    assertThat(bap.getNumeroAcciones()).isNull();
    assertThat(bap.getAccion().getEmpresa()).isEqualTo("Credicorp Ltd.");
  }
}
