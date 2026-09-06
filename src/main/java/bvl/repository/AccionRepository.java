package bvl.repository;

import bvl.domain.Accion;
import org.springframework.data.repository.CrudRepository;

public interface AccionRepository extends CrudRepository<Accion, Long> {

  Accion findByNemonico(String nemonico);
}
