package bvl.schedule;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Horario de sondeo derivado de las propiedades {@code horaInicio}, {@code horaFin} e
 * {@code intervalo}.
 *
 * <p>Se sondea a la hora de inicio y luego cada intervalo <b>contado desde la hora de inicio</b>,
 * hasta la hora de fin incluida, de lunes a viernes: con 9:45 cada 20 minutos, a las 9:45, 10:05,
 * 10:25... {@link #siguienteSondeo(LocalDateTime)} calcula el siguiente de esos instantes.
 *
 * <p>Hasta la 5.0.2 esto era un cron de Spring, que solo sabe disparar sobre la rejilla del reloj
 * (:00, :20, :40): la hora de inicio servia para descartar disparos, no para marcar la cadencia, y
 * con una hora de inicio que no fuera multiplo del intervalo el primer sondeo caia en el siguiente
 * tick de la rejilla. Por el mismo motivo el intervalo tenia que dividir a 60; ya no hace falta.
 *
 * <p>Todas las horas son de {@link #ZONA_MERCADO}, no de la zona del equipo.
 *
 * <p>Se valida al construir para que un properties mal puesto reviente al arrancar y no a mitad de
 * sesion.
 */
public final class HorarioSondeo {

    /** Las horas de la ventana son hora de la BVL, no del equipo donde corra la aplicacion. */
    public static final ZoneId ZONA_MERCADO = ZoneId.of("America/Lima");

    /** Acepta "9:40:00" y "16:30": hora de uno o dos digitos y segundos opcionales. */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm[:ss]");

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
        return new HorarioSondeo(inicio, fin, parseIntervalo(intervalo, Duration.between(inicio, fin)));
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

    /**
     * Minutos enteros, al menos uno, y que quepan en la franja: un intervalo mas largo que la franja
     * no llegaria nunca al segundo sondeo del dia, y seguramente es un error al teclear.
     */
    private static int parseIntervalo(String intervalo, Duration franja) {
        Duration d = parseDuracion(intervalo, "intervalo");
        if (d.toSecondsPart() != 0) {
            throw new IllegalArgumentException(
                    "La propiedad intervalo ('" + intervalo + "') debe ser minutos enteros, sin segundos");
        }
        if (d.isZero()) {
            throw new IllegalArgumentException(
                    "La propiedad intervalo ('" + intervalo + "') debe ser de al menos un minuto");
        }
        if (d.compareTo(franja) > 0) {
            throw new IllegalArgumentException(
                    "La propiedad intervalo ('" + intervalo + "') no cabe en la franja de horaInicio a "
                            + "horaFin, que dura " + franja.toMinutes() + " min");
        }
        return (int) d.toMinutes();
    }

    /**
     * El primer sondeo estrictamente posterior a {@code despuesDe}, en hora de
     * {@link #ZONA_MERCADO}.
     *
     * <p>Estrictamente posterior porque quien pregunta suele ser un disparo que acaba de ejecutarse
     * (con unos milisegundos de retraso): devolver ese mismo instante lo repetiria.
     */
    public LocalDateTime siguienteSondeo(LocalDateTime despuesDe) {
        LocalDate dia = despuesDe.toLocalDate();
        if (esDiaDeMercado(dia)) {
            LocalDateTime hoy = siguienteDelDia(dia, despuesDe);
            if (hoy != null) {
                return hoy;
            }
        }
        do {
            dia = dia.plusDays(1);
        } while (!esDiaDeMercado(dia));
        return dia.atTime(inicio);
    }

    /** El siguiente sondeo de ese dia, o {@code null} si ya no queda ninguno antes de la hora de fin. */
    private LocalDateTime siguienteDelDia(LocalDate dia, LocalDateTime despuesDe) {
        LocalDateTime primero = dia.atTime(inicio);
        if (despuesDe.isBefore(primero)) {
            return primero;
        }
        long intervalo = intervaloMinutos * 60L;
        long transcurridos = Duration.between(primero, despuesDe).toSeconds();
        LocalDateTime siguiente = primero.plusSeconds((transcurridos / intervalo + 1) * intervalo);
        return siguiente.isAfter(dia.atTime(fin)) ? null : siguiente;
    }

    private static boolean esDiaDeMercado(LocalDate dia) {
        DayOfWeek d = dia.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
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
                + " min, hora de Lima}";
    }
}
