package bvl.ui;

import static org.assertj.core.api.Assertions.assertThat;

import bvl.schedule.HorarioSondeo;
import java.awt.Component;
import java.time.LocalTime;
import javax.swing.JToggleButton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El formulario de la ventana. Es un {@code JPanel} y no una ventana, asi que se prueba sin
 * pantalla, que es como corre la suite.
 */
class PanelSondeoTest {

  private static final PanelSondeo.Acciones SIN_ACCIONES = new PanelSondeo.Acciones() {
    @Override
    public void aplicarHorario() {
    }

    @Override
    public void iniciar() {
    }

    @Override
    public void detener() {
    }

    @Override
    public void mostrarDatos() {
    }

    @Override
    public void exportar() {
    }
  };

  private static JToggleButton botonIniciar(PanelSondeo panel) {
    for (Component c : panel.getComponents()) {
      if (c instanceof JToggleButton boton) {
        return boton;
      }
    }
    throw new AssertionError("el panel no tiene boton Iniciar");
  }

  @Test
  @DisplayName("si no se llega a sondear, el boton vuelve a Iniciar y sin pulsar")
  void noSondearDesmarcaElBoton() {
    // Es un interruptor: el clic ya lo dejo pulsado. Si Iniciar se cancela porque el horario no
    // vale, cambiar solo el texto dejaria un boton que dice Iniciar pero esta pulsado, y el
    // siguiente clic lo interpretaria como Detener.
    PanelSondeo panel = new PanelSondeo(SIN_ACCIONES);
    JToggleButton boton = botonIniciar(panel);
    boton.setSelected(true);

    panel.marcarSondeando(false);

    assertThat(boton.getText()).isEqualTo("Iniciar");
    assertThat(boton.isSelected()).isFalse();
  }

  @Test
  @DisplayName("mientras sondea, el boton queda pulsado y dice Detener")
  void sondeandoMarcaElBoton() {
    PanelSondeo panel = new PanelSondeo(SIN_ACCIONES);

    panel.marcarSondeando(true);

    assertThat(botonIniciar(panel).getText()).isEqualTo("Detener");
    assertThat(botonIniciar(panel).isSelected()).isTrue();
  }

  @Test
  @DisplayName("lo que muestra el panel se vuelve a leer igual, tambien con intervalos de mas de una hora")
  void mostrarYLeerSonSimetricos() {
    PanelSondeo panel = new PanelSondeo(SIN_ACCIONES);
    HorarioSondeo horario = HorarioSondeo.of("9:45:00", "16:30", "01:30:00");

    panel.mostrarHorario(horario);
    HorarioSondeo leido = panel.leerHorario();

    assertThat(leido.getInicio()).isEqualTo(LocalTime.of(9, 45));
    assertThat(leido.getFin()).isEqualTo(LocalTime.of(16, 30));
    assertThat(leido.getIntervaloMinutos()).isEqualTo(90);
  }
}
