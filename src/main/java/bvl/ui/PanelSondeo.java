package bvl.ui;

import bvl.schedule.HorarioSondeo;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/**
 * El formulario de la ventana: horario, umbral de alarma y los tres botones.
 *
 * <p>Solo sabe de campos y de texto. Que hacer cuando el usuario pulsa algo lo decide
 * {@link VentanaPrincipal} a traves de {@link Acciones}, para que la maqueta no arrastre logica de
 * sondeo.
 */
public class PanelSondeo extends JPanel {

    /** Lo que la ventana sabe hacer cuando el usuario interactua con el formulario. */
    public interface Acciones {

        /** Enter en cualquier campo de horario: se aplica en caliente. */
        void aplicarHorario();

        void iniciar();

        void detener();

        void mostrarDatos();

        void exportar();
    }

    private final JTextField txtHoraInicio = new JTextField();
    private final JTextField txtHoraFin = new JTextField();
    private final JTextField txtIntervalo = new JTextField();
    private final JTextField txtAlarma = new JTextField();
    private final JToggleButton btnIniciar = new JToggleButton("Iniciar");
    private final JButton btnMostrar = new JButton("Mostrar");
    private final JButton btnExportar = new JButton("Exportar");

    public PanelSondeo(Acciones acciones) {
        setLayout(new GridLayout(6, 2, 10, 10));

        add(new JLabel("Hora de Inicio"));
        add(txtHoraInicio);
        add(new JLabel("Hora de Fin"));
        add(txtHoraFin);
        add(new JLabel("Intervalo"));
        add(txtIntervalo);
        add(new JLabel("Variación (%):"));
        add(txtAlarma);
        add(btnMostrar);
        add(btnIniciar);
        add(btnExportar);

        txtHoraInicio.addActionListener(e -> acciones.aplicarHorario());
        txtHoraFin.addActionListener(e -> acciones.aplicarHorario());
        txtIntervalo.addActionListener(e -> acciones.aplicarHorario());

        btnIniciar.addActionListener(e -> {
            if (btnIniciar.isSelected()) {
                acciones.iniciar();
            } else {
                acciones.detener();
            }
        });
        btnMostrar.addActionListener(e -> acciones.mostrarDatos());
        btnExportar.addActionListener(e -> acciones.exportar());
    }

    /** Vuelca un horario a los campos, con el mismo formato que se espera al leerlos. */
    public void mostrarHorario(HorarioSondeo horario) {
        txtHoraInicio.setText(horario.getInicio().toString());
        txtHoraFin.setText(horario.getFin().toString());
        txtIntervalo.setText(String.format("%02d:%02d:00", horario.getIntervaloMinutos() / 60,
                horario.getIntervaloMinutos() % 60));
    }

    /**
     * Lee el horario tecleado.
     *
     * @throws IllegalArgumentException si lo escrito no forma un horario valido
     */
    public HorarioSondeo leerHorario() {
        return HorarioSondeo.of(txtHoraInicio.getText(), txtHoraFin.getText(),
                txtIntervalo.getText());
    }

    /** Se muestra en crudo, tal y como viene de las propiedades. */
    public void mostrarAlarma(String alarma) {
        txtAlarma.setText(alarma);
    }

    /**
     * Umbral de variacion tecleado.
     *
     * @throws NumberFormatException si lo escrito no es un numero
     */
    public double leerAlarma() {
        return Double.parseDouble(txtAlarma.getText());
    }

    /** El boton es un interruptor: mientras sondea dice Detener. */
    public void marcarSondeando(boolean sondeando) {
        btnIniciar.setText(sondeando ? "Detener" : "Iniciar");
    }
}
