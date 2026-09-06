package bvl.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Lectura {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(insertable = true, updatable = true, unique = true, nullable = false)
    private LocalDateTime fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
