package bvl;

import bvl.domain.Item;
import bvl.service.BvlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BVL2 {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    BvlService bvlService;
    @Autowired
    private BvlReader reader;
    private List<Item> data;
    private List<Item> dataAnt;

    public static void main(String[] args) {
        final BVL2 bvl = new BVL2();
        new Thread() {
            // @Override
            public void run() {
                while (true) {
                    try {
                        System.out.println("INICIANDO LECTURA...");
                        bvl.process();
                        System.out.println("LECTURA FINALIZADA!");
                        sleep(18 * 60 * 1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void process() {
        LocalDateTime lastDate = bvlService.getLastDate();
        if (lastDate == null) {
            lastDate = LocalDateTime.MIN;
        }

        // last = "2008-01-23 15:54:16.0";
        // last = "2008-01-23 14:14:14";
        logger.debug("Last reading stored date: " + lastDate);
        dataAnt = bvlService.getItems(lastDate);
        data = reader.readData();
        LocalDateTime date = reader.getFecha();
        logger.debug("Last reading published date: " + date);

//    if (date != null && lastDate.isBefore(date)) {
        bvlService.saveData(data, date);
        lastDate = bvlService.getLastDate();
        bvlService.exportar(date, date);
//    }
    }

    public String[] getVariaciones(double variacion) {
        String msg = "";

        String alertas = "<b>Las siguientes empresas variaron:</b><br /><br />";

        for (int i = 0; i < data.size(); i++) {
            Item item = data.get(i);

            String name = item.getAccion().getNemonico();

            Double delta = item.getVariacionPorcentual();
            if (delta == null) {
                continue;
            }
            System.out.println("VAR=" + delta);

            NumberFormat nf = DecimalFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            String strDelta = nf.format(delta);

            String updown = " bajó ";
            String plusminus = " - ";
            if (delta > 0) {
                updown = " subió ";
                plusminus = " + ";
            }
            if (Math.abs(delta) >= variacion) {
                String str = plusminus + name + updown + strDelta + "%";
                msg += str + "\n";
                if (delta > 0) {
                    str = "<div style='color:blue'>" + str + "</div>";
                } else {
                    str = "<div style='color:red'>" + str + "</div>";
                }
                alertas += str;
            }
        }
        if (msg.equals("")) {
            return new String[]{"Sin variaciones.", "Sin variaciones."};
        }
        final String alertas2 = "<html>" + alertas + "</html>";
        // new Thread() {
        // public void run() {
        // try {sleep(1000L);}catch(Exception e){e.printStackTrace();}
        // javax.swing.JOptionPane.showMessageDialog(null, alertas2, "Alertas",
        // 0);
        // }
        // }.start();
        return new String[]{msg, alertas2};
    }

    public BvlService getConnection() {
        return bvlService;
    }

    public LocalDateTime getFecha() {
        return reader.getFecha();
    }
}
