package bvl.bean;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

@Deprecated
public class LastReadBean {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
  private LocalTime hora;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  private LocalDate fechaSiguiente;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
  private LocalDate fechaCalendario;

  public LocalTime getHora() {
    return hora;
  }

  public void setHora(LocalTime hora) {
    this.hora = hora;
  }

  public LocalDate getFechaSiguiente() {
    return fechaSiguiente;
  }

  public void setFechaSiguiente(LocalDate fechaSiguiente) {
    this.fechaSiguiente = fechaSiguiente;
  }

  public LocalDate getFechaCalendario() {
    return fechaCalendario;
  }

  public void setFechaCalendario(LocalDate fechaCalendario) {
    this.fechaCalendario = fechaCalendario;
  }
}
