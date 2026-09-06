package bvl.config;

import bvl.alert.AlertaFormatter;
import bvl.alert.DetectorVariaciones;
import bvl.schedule.BvlScheduler;
import bvl.ui.VentanaPrincipal;
import java.awt.GraphicsEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Construye y muestra la ventana.
 *
 * <p>Vive aparte de {@code JavaBvlApplication} para que un contexto recortado —los
 * {@code @DataJpaTest}, por ejemplo— pueda arrancar sin tener que poder construir un
 * {@link javax.swing.JFrame} ni las dependencias que este exige.
 *
 * <p>Las dependencias entran por parametro del metodo, no por campo: asi el bean se declara con lo
 * que realmente necesita y se ve de un vistazo.
 *
 * <p>La ventana solo se construye si hay pantalla. En produccion siempre la hay:
 * {@code JavaBvlApplication} fuerza {@code headless(false)} antes de levantar el contexto. En la
 * suite, que corre headless, la condicion no se cumple y el contexto puede arrancar entero sin
 * intentar abrir un {@code JFrame}.
 */
@Configuration
@Conditional(SwingConfiguration.HayPantalla.class)
public class SwingConfiguration {

    static class HayPantalla implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !GraphicsEnvironment.isHeadless();
        }
    }

    @Bean
    public VentanaPrincipal ventanaPrincipal(BvlScheduler scheduler,
                                             DetectorVariaciones detector,
                                             AlertaFormatter formatter,
                                             BvlProperties properties) {
        VentanaPrincipal ventana = new VentanaPrincipal(scheduler, detector, formatter, properties);
        ventana.setVisible(true);
        return ventana;
    }
}
