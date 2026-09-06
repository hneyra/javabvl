package bvl;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Arranque de la aplicacion.
 *
 * <p>{@code headless(false)} no es opcional: la aplicacion es una ventana Swing y sin esto Boot
 * arrancaria en modo headless y el {@code JFrame} reventaria. Los tests, en cambio, corren
 * headless: lo fija surefire en el POM.
 *
 * <p>La aplicacion arranca <b>en reposo</b>. Hasta que alguien pulsa Iniciar no se programa ni se
 * ejecuta ningun sondeo.
 */
@SpringBootApplication
public class JavaBvlApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(JavaBvlApplication.class).headless(false).run(args);
    }
}
