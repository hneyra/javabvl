package bvl.repository;

import bvl.domain.Lectura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LecturaRepository extends CrudRepository<Lectura, Long> {

    Lectura findByFecha(LocalDateTime fecha);

    List<Lectura> findByFechaBetween(LocalDateTime fecha1, LocalDateTime fecha2);

    Page<Lectura> findAll(Pageable page);
}
