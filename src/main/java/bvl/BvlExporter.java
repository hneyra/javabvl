package bvl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import bvl.service.BvlService;
import org.apache.poi.hssf.util.HSSFColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bvl.domain.Item;

public class BvlExporter {

  final static String[] MONTHS = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
      "Agosto",
      "Setiembre", "Octubre", "Noviembre", "Diciembre"};

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public final static String EXTENSION = "xls";
  public static String xlsTemplatePath = "/res/template." + EXTENSION;
  // public static String xlsTemplatePath = "/tmp/template.xls";

  private XlsWriter xlsWriter;

  private File file;

  String xlsPath;

  public BvlExporter(String xlsPath) {
    this.xlsPath = xlsPath;
  }

  public void createXLSDay(Date date) {

    String path = xlsPath;
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    path += c.get(Calendar.YEAR) + File.separator + MONTHS[c.get(Calendar.MONTH)] + File.separator;

    String fileName = c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) > 8 ? "" : "0")
        + (c.get(Calendar.MONTH) + 1) + "." + (c.get(Calendar.DAY_OF_MONTH) > 9 ? "" : "0")
        + c.get(Calendar.DAY_OF_MONTH);

    SimpleDateFormat formatter = new SimpleDateFormat("HH.mm.ss");
    String sheetName = formatter.format(date);

    createXls(sheetName, fileName, path, date);
  }

  public void createXlsMonth(Date date) {
    String path = xlsPath;
    Calendar c = Calendar.getInstance();
    c.setTime(date);
    path += c.get(Calendar.YEAR) + File.separator;

    String fileName = c.get(Calendar.YEAR) + "." + (c.get(Calendar.MONTH) + 1 > 9 ? "" : "0")
        + (c.get(Calendar.MONTH) + 1) + "_" + MONTHS[c.get(Calendar.MONTH)];

    String sheetName = (c.get(Calendar.DAY_OF_MONTH) > 9 ? "" : "0") + c.get(Calendar.DAY_OF_MONTH);
    createXls(sheetName, fileName, path, date);
  }

  private void createXls(String sheetName, String fileName, String path, Date date) {

    file = new File(path);

    if (file.mkdirs()) {
      logger.info("Se creo el directorio: " + file.getPath());
    }
    file = new File(file, fileName + "." + EXTENSION);
    logger.info("Creating file in " + file.getAbsolutePath());
    try {
      if (file.createNewFile()) {
        logger.info("Se creo el archivo: " + file.getName());
        cargarPlantilla(file);
      }
    } catch (IOException ioe) {
      logger.error(ioe.getMessage(), ioe);
    }

    // path += name;
    // file = fileChooser.getSelectedFile();
    xlsWriter = new XlsWriter(file);
    createSheet(sheetName, date);
  }

  public void cargarPlantilla(File file) {
    InputStream is = null;
    OutputStream os = null;
    try {
      is = getClass().getResourceAsStream(xlsTemplatePath);
      os = new FileOutputStream(file);

      int x = is.read();

      while (x != -1) {
        os.write(x);
        x = is.read();
      }
    } catch (IOException e) {
      logger.error("Error al leer la plantilla: " + xlsTemplatePath + " : " + e.getMessage(), e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
        }
      }
    }

  }

  public void writeData(List<Item> data) {
    for (Item item : data) {
      xlsWriter.addCell(item.getAccion().getNemonico());
      xlsWriter.addCell(item.getCotizacionAnterior());
      xlsWriter.addCell(item.getFechaAnterior());
      xlsWriter.addCell(item.getCotizacionApertura());
      xlsWriter.addCell("-------");
      xlsWriter.addCell(item.getCotizacionUltima());
      xlsWriter.addCell(item.getPropuestaCompra());
      xlsWriter.addCell(item.getPropuestaVenta());
      xlsWriter.addCell(item.getNumeroAcciones());
      xlsWriter.addCell(item.getMontoNegociado());
      xlsWriter.addCell(item.getNumeroOperaciones());
      xlsWriter.addCell(item.getMoneda().getNombre());
      xlsWriter.addCell(item.getAccion().getSector().getNombre());
      xlsWriter.addCell(item.getVariacionPorcentual());
      xlsWriter.addRow();
    }
    xlsWriter.write();
  }

  public void createSheet(String sheetName, Date date) {
    xlsWriter.createSheet(sheetName);
    xlsWriter.setCurrentSheet(sheetName);

    xlsWriter.addRow();
    xlsWriter.addCell(BvlService.sdf.format(date));
    xlsWriter.addRow();
    xlsWriter.addCell("ACCIONES");
    xlsWriter.setColumnWidth((short) 4000);
    xlsWriter.mergeCell(1, (short) 0, 2, (short) 0);
    xlsWriter.createCellStyle();
    xlsWriter.useStyleAlign();
    xlsWriter.useStyle2();
    xlsWriter.useStyle3(HSSFColor.HSSFColorPredefined.GREY_80_PERCENT.getIndex());
    xlsWriter.applyStyle();

    xlsWriter.addCell("PRECIOS");
    xlsWriter.setColumnWidth((short) 2000);
    xlsWriter.createCellStyle();
    xlsWriter.useStyleAlign();
    xlsWriter.useStyle2();
    xlsWriter.useStyle3(HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex());
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 1, 1, (short) 5);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("PROPUESTAS", (short) (xlsWriter.getCurrentColumnPos() + 4));
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 6, 1, (short) 7);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("No.ACC o GRUPOS", (short) (xlsWriter.getCurrentColumnPos() + 1));
    xlsWriter.setColumnWidth((short) 4000);
    xlsWriter.createCellStyle();
    xlsWriter.useStyleAlign();
    xlsWriter.useStyle2();
    xlsWriter.useStyle3(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT.getIndex());
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 8, 2, (short) 8);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("MONTO NEGOCIADO");
    xlsWriter.setColumnWidth((short) 4500);
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 9, 2, (short) 9);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("No. OPE.");
    xlsWriter.setColumnWidth((short) 2000);
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 10, 2, (short) 10);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("MON EDA");
    xlsWriter.setColumnWidth((short) 2000);
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 11, 2, (short) 11);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("SEC TOR");
    xlsWriter.setColumnWidth((short) 2000);
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 12, 2, (short) 12);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addCell("VARIA CION");
    xlsWriter.setColumnWidth((short) 2000);
    xlsWriter.applyStyle();
    xlsWriter.mergeCell(1, (short) 13, 2, (short) 13);
    xlsWriter.useMergedStyle(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

    xlsWriter.addRow();

    xlsWriter.addCell("ANT", (short) 1);
    xlsWriter.createCellStyle();
    xlsWriter.useStyleAlign();
    xlsWriter.useStyle2();
    xlsWriter.useStyle3(HSSFColor.HSSFColorPredefined.LIGHT_ORANGE.getIndex());
    xlsWriter.applyStyle();

    xlsWriter.addCell("A.FEC");
    xlsWriter.setColumnWidth((short) 2800);
    xlsWriter.applyStyle();
    xlsWriter.addCell("APE");
    xlsWriter.applyStyle();
    xlsWriter.addCell("EX-DER");
    xlsWriter.applyStyle();
    xlsWriter.addCell("ULT");
    xlsWriter.applyStyle();
    xlsWriter.addCell("COM");
    xlsWriter.applyStyle();
    xlsWriter.addCell("VEN");
    xlsWriter.applyStyle();
    xlsWriter.addRow();
  }

  public XlsWriter getXls() {
    return xlsWriter;
  }

  public void setXls(XlsWriter xls) {
    this.xlsWriter = xls;
  }

  public void closeResources() {
//     xlsWriter.closeResources();
  }
}
