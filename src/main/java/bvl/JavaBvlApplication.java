package bvl;

import bvl.domain.Accion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@EnableAutoConfiguration
@EntityScan(basePackageClasses = Accion.class)
@SpringBootApplication
public class JavaBvlApplication {

    @Autowired
    BVL2 bvl;

    public static void main(String[] args) {
        new SpringApplicationBuilder(JavaBvlApplication.class).headless(false).run(args);
    }

    @Bean
    public JBVL frame() {
        JBVL jbvl = new JBVL(bvl);
        jbvl.setVisible(true);
        return jbvl;
    }
}
