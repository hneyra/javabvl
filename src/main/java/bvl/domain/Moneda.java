package bvl.domain;

/** Moneda en la que cotiza una accion ("S/", "US$"). */
public class Moneda {

    private String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
