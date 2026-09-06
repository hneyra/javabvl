package bvl.repository;

import bvl.domain.Moneda;
import org.springframework.data.repository.CrudRepository;

public interface MonedaRepository extends CrudRepository<Moneda, Long> {

    Moneda findByNombre(String nombre);
}
