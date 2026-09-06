package bvl.market;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Desactiva la validacion de certificados TLS de {@link HttpsURLConnection} para toda la JVM.
 *
 * <p>Es deliberado: el endpoint de la BVL ha servido historicamente cadenas que la JVM rechaza, y
 * sin esto la aplicacion no lee nada. Vive en su propia clase, y no escondido en el lector, por dos
 * razones: que un efecto global de este calibre sea visible al listar el paquete, y que los tests
 * del lector puedan construirlo sin arrastrar el efecto a la JVM de la suite.
 *
 * <p>Alcance real: solo {@code HttpsURLConnection}. No toca al cliente Reactor Netty que usa
 * {@link BvlClient}.
 */
@Component
public class TlsInseguro {

    private static final Logger logger = LoggerFactory.getLogger(TlsInseguro.class);

    private static final TrustManager[] ACEPTA_CUALQUIER_CERTIFICADO = {new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] cadena, String tipoAutenticacion) {
            // Sin validacion, a proposito.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] cadena, String tipoAutenticacion) {
            // Sin validacion, a proposito.
        }
    }};

    @PostConstruct
    public void desactivarValidacion() {
        try {
            SSLContext contexto = SSLContext.getInstance("TLS");
            contexto.init(null, ACEPTA_CUALQUIER_CERTIFICADO, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(contexto.getSocketFactory());
            logger.info("Validacion de certificados TLS desactivada para HttpsURLConnection");
        } catch (GeneralSecurityException e) {
            logger.error("No se pudo desactivar la validacion TLS: {}", e.getMessage(), e);
        }
    }
}
