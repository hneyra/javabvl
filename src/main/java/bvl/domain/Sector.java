package bvl.domain;

import jakarta.persistence.*;

@Entity
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(insertable = true, updatable = true, unique = true, nullable = false, length = 64)
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

    public void setNombre(String nombre) {
        this.nombre = nombre == null ? "---" : nombre;
    }
}
