package bvl.support;

import bvl.domain.Accion;
import bvl.repository.LecturaRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuracion de arranque para los tests de persistencia.
 *
 * <p>Hace falta porque {@code @DataJpaTest} busca hacia arriba una {@code @SpringBootConfiguration}
 * y encuentra {@code JavaBvlApplication}, que declara un {@code @Autowired BVL2} y un bean
 * {@code JBVL} (JFrame). Ninguno de los dos existe en un contexto recortado a JPA, asi que el
 * contexto no arranca. Los tests referencian esta clase con {@code @ContextConfiguration}.
 *
 * <p>Vive en {@code bvl.support} a proposito: fuera de la ruta de busqueda ascendente de los tests,
 * para no provocar un "Found multiple @SpringBootConfiguration".
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = Accion.class)
@EnableJpaRepositories(basePackageClasses = LecturaRepository.class)
public class TestJpaConfig {
}
