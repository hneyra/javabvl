package bvl.schedule;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Horario de sondeo derivado de las propiedades {@code horaInicio}, {@code horaFin} e
 * {@code intervalo}.
 *
 * <p>Un cron no sabe expresar "cada N minutos entre las 9:40 y las 16:30", asi que el horario se
 * parte en dos: {@link #toCron()} genera la rejilla del reloj dentro del rango de horas, y
 * {@link #dentroDeVentana(LocalTime)} descarta los disparos que caen fuera del horario real de la
 * sesion. Ejemplo con los valores de produccion (9:40 - 16:30 cada 20 min): el cron dispara tambien
 * a las 9:00 y 9:20 porque la hora 9 entra entera en el rango, y la ventana los descarta.
 *
 * <p>El intervalo tiene que ser un numero entero de minutos divisor de 60 para que la rejilla sea
 * regular. Se valida al construir para que un properties mal puesto reviente al arrancar y no a
 * mitad de sesion.
 */
public final class HorarioSondeo {

    /** Acepta "9:40:00" y "16:30": hora de uno o dos digitos y segundos opcionales. */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private static final String DIAS_DE_MERCADO = "MON-FRI";

    private final LocalTime inicio;
    private final LocalTime fin;
    private final int intervaloMinutos;

    private HorarioSondeo(LocalTime inicio, LocalTime fin, int intervaloMinutos) {
        this.inicio = inicio;
        this.fin = fin;
        this.intervaloMinutos = intervaloMinutos;
    }

    public static HorarioSondeo of(String horaInicio, String horaFin, String intervalo) {
        LocalTime inicio = parseHora(horaInicio, "horaInicio");
        LocalTime fin = parseHora(horaFin, "horaFin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "horaFin (" + horaFin + ") debe ser posterior a horaInicio (" + horaInicio + ")");
        }
        return new HorarioSondeo(inicio, fin, parseIntervalo(intervalo));
    }

    private static LocalTime parseHora(String valor, String propiedad) {
        try {
            return LocalTime.parse(valor.trim(), FORMATO_HORA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "La propiedad " + propiedad + " no tiene un formato valido: '" + valor
                            + "'. Formato esperado H:mm o H:mm:ss", e);
        }
    }

    /**
     * Lee una propiedad de duracion escrita como {@code hh:mm:ss}, el mismo formato que usan las
     * horas del fichero. Publico porque {@code alertaTimeout} se lee igual.
     */
    public static Duration parseDuracion(String valor, String propiedad) {
        return Duration.between(LocalTime.MIDNIGHT, parseHora(valor, propiedad));
    }

    private static int parseIntervalo(String intervalo) {
        Duration d = parseDuracion(intervalo, "intervalo");
        if (d.toSecondsPart() != 0) {
            throw new IllegalArgumentException(
                    "La propiedad intervalo ('" + intervalo + "') debe ser minutos enteros, sin segundos");
        }
        long minutos = d.toMinutes();
        if (minutos < 1 || minutos > 60) {
            throw new IllegalArgumentException(
                    "La propiedad intervalo ('" + intervalo + "') debe estar entre 00:01:00 y 01:00:00");
        }
        if (60 % minutos != 0) {
            throw new IllegalArgumentException(
                    "La propiedad intervalo ('" + intervalo + "') debe ser un divisor de 60 minutos "
                            + "(1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30 o 60) para generar una rejilla regular");
        }
        return (int) minutos;
    }

    /** Expresion cron de Spring: {@code segundo minuto hora dia-mes mes dia-semana}. */
    public String toCron() {
        String minutos = intervaloMinutos == 60 ? "0" : "0/" + intervaloMinutos;
        String horas = inicio.getHour() == fin.getHour()
                ? String.valueOf(inicio.getHour())
                : inicio.getHour() + "-" + fin.getHour();
        return "0 " + minutos + " " + horas + " * * " + DIAS_DE_MERCADO;
    }

    /** Ventana real de la sesion, con ambos extremos incluidos. */
    public boolean dentroDeVentana(LocalTime hora) {
        return !hora.isBefore(inicio) && !hora.isAfter(fin);
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public LocalTime getFin() {
        return fin;
    }

    public int getIntervaloMinutos() {
        return intervaloMinutos;
    }

    @Override
    public String toString() {
        return "HorarioSondeo{" + inicio + " - " + fin + " cada " + intervaloMinutos
                + " min, cron='" + toCron() + "'}";
    }
}
