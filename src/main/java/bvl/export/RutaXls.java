package bvl.export;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Donde va cada fichero y como se llama cada hoja.
 *
 * <p>Todo el arbol que ve el usuario final se decide aqui:
 * <pre>
 *   &lt;raiz&gt;/2024/Enero/2024.01.15.xls   una hoja por hora  ("10.30.00")
 *   &lt;raiz&gt;/2024/2024.01_Enero.xls      una hoja por dia   ("15")
 * </pre>
 *
 * <p>Los nombres de mes son literales y no salen del {@code Locale}: la BVL usa "Setiembre", que no
 * es lo que produciria un formateador en castellano, y los ficheros de anios anteriores ya estan
 * nombrados asi.
 */
public final class RutaXls {

    public static final String EXTENSION = "xls";

    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"};

    private static final DateTimeFormatter NOMBRE_DIARIO = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter NOMBRE_MENSUAL = DateTimeFormatter.ofPattern("yyyy.MM");
    private static final DateTimeFormatter HOJA_DIARIA = DateTimeFormatter.ofPattern("HH.mm.ss");
    private static final DateTimeFormatter HOJA_MENSUAL = DateTimeFormatter.ofPattern("dd");
    private static final DateTimeFormatter MARCA_DE_TIEMPO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String raiz;

    public RutaXls(String raiz) {
        this.raiz = raiz;
    }

    /** {@code <raiz>/<anio>/<Mes>/<aaaa.mm.dd>.xls} */
    public File ficheroDiario(LocalDateTime fecha) {
        File carpeta = new File(new File(raiz, anio(fecha)), mes(fecha));
        return new File(carpeta, NOMBRE_DIARIO.format(fecha) + "." + EXTENSION);
    }

    /** Una hoja por lectura dentro del fichero del dia. */
    public String hojaDiaria(LocalDateTime fecha) {
        return HOJA_DIARIA.format(fecha);
    }

    /** {@code <raiz>/<anio>/<aaaa.mm_Mes>.xls} */
    public File ficheroMensual(LocalDateTime fecha) {
        File carpeta = new File(raiz, anio(fecha));
        return new File(carpeta,
                NOMBRE_MENSUAL.format(fecha) + "_" + mes(fecha) + "." + EXTENSION);
    }

    /** Una hoja por dia dentro del fichero del mes. */
    public String hojaMensual(LocalDateTime fecha) {
        return HOJA_MENSUAL.format(fecha);
    }

    /** Fecha de la lectura tal y como se escribe en la primera celda de cada hoja. */
    public static String marcaDeTiempo(LocalDateTime fecha) {
        return MARCA_DE_TIEMPO.format(fecha);
    }

    private static String anio(LocalDateTime fecha) {
        return String.valueOf(fecha.getYear());
    }

    private static String mes(LocalDateTime fecha) {
        return MESES[fecha.getMonthValue() - 1];
    }
}
