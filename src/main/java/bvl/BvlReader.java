package bvl;

import bvl.domain.*;
import bvl.domain.input.BvlItem;
import bvl.domain.input.Daily;
import bvl.domain.input.StockMarket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BvlReader {

//    reactor.netty.http.client.HttpClientOperations hco;

    public static int DEFAULT_TIMEOUT_MS = 300_000;
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${urlCotizaciones}")
    private String urlCotizaciones;

    @Value("${urlHora}")
    private String urlHora;

    @Autowired
    private WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // public static void main(String[] args) {
    // new BVLReader().leerArchivo();
    // }

    public BvlReader() {
    }

    @PostConstruct
    public void init() {
        // register module in order to support LocalTime and LocalDate
        objectMapper.registerModule(new JavaTimeModule());
        disableSSLCertificateChecking();
    }

    String extractJson(String data) {
        final String startString = "data\t=\t{";
        final String endString = "}else{";
        int startPos = data.indexOf(startString);
        if (startPos != -1) {
            int endPos = data.indexOf(endString, startPos);
            if (endPos != -1) {
                return data.substring(startPos + startString.length() - 1, endPos);
            }
        }
        return null;
    }

    public List<Item> readData() {
        List<Item> ans = new ArrayList<>();
        //TODO: use ReadDataBean.timestamp instead of make request into getFecha
        LocalDateTime fechaLectura = getFecha();
        try {
            Map<String, Object> body = Map.of("sector", "", "isToday", Boolean.TRUE, "companyCode", "", "inputCompany", "");
            Mono<StockMarket> response = webClient.post().uri(urlCotizaciones)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).
                    body(Mono.just(body), Map.class).
                     retrieve().
                    bodyToMono(new ParameterizedTypeReference<StockMarket>() {
                    }).
                    doOnError(throwable -> throwable.printStackTrace());

            ans.addAll(response.map(
                    r -> {
                        logger.info("RESPUESTA RECIBIDA DE " + urlCotizaciones + ": " + r);
                        return r;
                    }
            ).block().getContent().stream().map(item ->
                    fromBvlItem(item, fechaLectura)
            ).collect(Collectors.toList()));

            logger.debug("---->TOTAL Datos leidos: " + ans.size());
            logger.debug("Datos leidos: " + ans);
        } catch (Throwable ce) {
            String msg = "No hay conexión con: http:\\www.bvl.com.pe\n\n"
                    + "Por favor revise si tiene conexión a internet\n"
                    + "o si dicha web está disponble.\n\nLuego reinicie" + " el programa.";
            JOptionPane.showMessageDialog(null, msg);
            logger.error("Error de Conexión: " + ce.getMessage(), ce);
            ce.printStackTrace();
        }
        return ans;
    }

    private Item fromBvlItem(BvlItem bvlItem, LocalDateTime fechaLectura) {
        Lectura lectura = new Lectura();
        lectura.setFecha(fechaLectura);

        Sector sector = new Sector();
        sector.setNombre(bvlItem.getSectorDescription());

        Accion accion = new Accion();
        accion.setEmpresa(bvlItem.getCompanyName());
        accion.setNemonico(bvlItem.getNemonico());
        accion.setSector(sector);

        Moneda moneda = new Moneda();
        moneda.setNombre(bvlItem.getCurrency());

        Item item = new Item();
        item.setAccion(accion);
        item.setSegmento(bvlItem.getSectorCode() == null ? "" : bvlItem.getSectorCode());
        item.setMoneda(moneda);
        item.setLectura(lectura);
        item.setFechaLectura(fechaLectura);
        item.setCotizacionAnterior(bvlItem.getPrevious());
        item.setFechaAnterior(bvlItem.getPreviousDate());
        item.setCotizacionApertura(bvlItem.getOpening());
        item.setCotizacionUltima(bvlItem.getLast());
        item.setVariacionPorcentual(bvlItem.getPercentageChange());
        item.setPropuestaCompra(bvlItem.getBuy());
        item.setPropuestaVenta(bvlItem.getSell());
        item.setNumeroAcciones(bvlItem.getNegotiatedQuantity());
        item.setNumeroOperaciones(bvlItem.getOperationsNumber());
        item.setMontoNegociado(bvlItem.getNegotiatedAmount());

        return item;
    }

    public LocalDateTime getFecha() {
        try {
            Mono<Daily> daily = webClient.get().uri(urlHora).retrieve().bodyToMono(Daily.class).doOnError(throwable -> throwable.printStackTrace());
            LocalDateTime fecha = daily.block().getUpdatedDate();
            logger.debug("Fecha de lectura: " + fecha);
            return fecha;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private String getFechaInicio(String text) {
        int pos = text.indexOf("n: ");
        if (pos != -1) {
            int lastPos = text.indexOf(" ", pos + 3);
            if (lastPos != -1) {
                return text.substring(pos + 3, lastPos);
            }
        }
        return null;
    }

    private String getHoraInicio(String text) {
        int pos = text.lastIndexOf(" ");
        if (pos != -1) {
            return text.substring(pos + 1, text.length());
        }
        return null;
    }

    private static void disableSSLCertificateChecking() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                // Not implemented
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
                // Not implemented
            }
        }};

        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String forceTrim(String text) {
        if (text != null) {
            return text.replace(String.valueOf((char) 160), " ").trim();
        }
        return null;
    }

    private LocalDate parseLocalDate(String value) {
        if (value != null && !value.isEmpty()) {
            return LocalDate.parse(value, dateFormatter);
        }
        return null;
    }

    private Long parseLong(String value) {
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value.replace(",", ""));
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value != null && !value.isEmpty()) {
            return Double.parseDouble(value.replace(",", ""));
        }
        return null;
    }
}
