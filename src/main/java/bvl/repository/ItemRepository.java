package bvl.repository;

import bvl.domain.Item;
import bvl.domain.Lectura;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ItemRepository extends CrudRepository<Item, Long> {

  List<Item> findByLectura(Lectura lectura);

  List<Item> findByLecturaIn(List<Lectura> lecturas);
}
