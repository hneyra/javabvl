package bvl.repository;

import bvl.domain.Sector;
import org.springframework.data.repository.CrudRepository;

public interface SectorRepository extends CrudRepository<Sector, Long> {

  Sector findByNombre(String nombre);
}
