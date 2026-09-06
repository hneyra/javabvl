package bvl.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Accion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(insertable = true, updatable = true, unique = true, nullable = false, length = 16)
    private String nemonico;

    @Column(insertable = true, updatable = true, unique = false, nullable = false, length = 128)
    private String empresa;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "sector_id", referencedColumnName = "id", updatable = true, insertable = true, unique = false)
    private Sector sector;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNemonico() {
        return nemonico;
    }

    public void setNemonico(String nemonico) {
        this.nemonico = nemonico;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }
}
