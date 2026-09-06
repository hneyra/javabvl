package bvl.repository;

import bvl.domain.Item;
import bvl.domain.Lectura;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ItemRepository extends CrudRepository<Item, Long> {

    List<Item> findByLectura(Lectura lectura);

    List<Item> findByLecturaIn(List<Lectura> lecturas);
}
