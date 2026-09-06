package bvl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String nombre;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Normaliza el nulo a {@code "---"}: la columna es NOT NULL y la BVL publica acciones sin
     * sector asignado.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre == null ? "---" : nombre;
    }
}
