package bvl.domain;

/** Sector economico al que la BVL adscribe una empresa. */
public class Sector {

    private String nombre;

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
