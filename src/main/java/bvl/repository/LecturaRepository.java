package bvl.repository;

import bvl.domain.Lectura;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface LecturaRepository extends CrudRepository<Lectura, Long> {

  Lectura findByFecha(LocalDateTime fecha);

  List<Lectura> findByFechaBetween(LocalDateTime fecha1, LocalDateTime fecha2);

  Page<Lectura> findAll(Pageable page);
}
