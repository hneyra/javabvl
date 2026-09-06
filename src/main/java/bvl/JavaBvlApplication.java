package bvl;

import bvl.domain.Accion;
import bvl.schedule.BvlScheduler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration
@EntityScan(basePackageClasses = Accion.class)
@SpringBootApplication
public class JavaBvlApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(JavaBvlApplication.class).headless(false).run(args);
    }

    // Las dependencias entran por parametro, no por campo @Autowired: asi esta clase no exige
    // beans que un contexto recortado (los @DataJpaTest) no tiene por que poder construir.
    @Bean
    public JBVL frame(BVL2 bvl, BvlScheduler scheduler) {
        JBVL jbvl = new JBVL(bvl, scheduler);
        jbvl.setVisible(true);
        return jbvl;
    }
}
