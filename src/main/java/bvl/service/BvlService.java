package bvl.service;

import bvl.BvlExporter;
import bvl.domain.*;
import bvl.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class BvlService {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  AccionRepository accionRepository;

  @Autowired
  ItemRepository itemRepository;

  @Autowired
  LecturaRepository lecturaRepository;

  @Autowired
  SectorRepository sectorRepository;

  @Autowired
  MonedaRepository monedaRepository;

  @Value("${xlsPath}")
  private String xlsPath;

  public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  public static SimpleDateFormat sdfd = new SimpleDateFormat("yyyy-MM-dd");
  public static SimpleDateFormat sdft = new SimpleDateFormat("HH:mm:ss");

  /**
   * Retorna las horas de ese dia
   */
  public Map<Long, LocalDateTime> getHoras(LocalDateTime fecha) {
    LocalDateTime ldt1 = LocalDateTime.from(fecha.toLocalDate());
    LocalDateTime ldt2 = LocalDateTime.from(ldt1.plusDays(1));

    List<Lectura> lecturas = lecturaRepository.findByFechaBetween(ldt1, ldt2);
    Map<Long, LocalDateTime> response = new HashMap<Long, LocalDateTime>();
    for (Lectura lectura : lecturas) {
      response.put(lectura.getId(), lectura.getFecha());
    }
    return response;
  }

  public List<Item> getItems(LocalDateTime fecha1, LocalDateTime fecha2) {
    List<Lectura> lecturas = lecturaRepository.findByFechaBetween(fecha1, fecha2);
    return itemRepository.findByLecturaIn(lecturas);
  }

  public List<Item> getItems(LocalDateTime fecha) {
    Lectura lectura = lecturaRepository.findByFecha(fecha);
    return itemRepository.findByLectura(lectura);

  }

  private String[] prepareValues(Object[] values) {
    String[] ans = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        ans[i] = "NULL";
      } else if (values[i] instanceof Date) {
        Date date = (Date) values[i];
        ans[i] = "TIMESTAMP('" + sdf.format(date) + "')";
      } else if (values[i] instanceof String) {
        ans[i] = "'" + values[i] + "'";
      } else if (values[i] instanceof Number) {
        ans[i] = values[i].toString();
      }
    }
    return ans;
  }

  public void saveData(List<Item> data, LocalDateTime fecha) {
    LocalDateTime lastDate = getLastDate();
//    if (lastDate != null) {
//      logger.info(fecha + "-->" + lastDate);
//    }
//    if (lastDate == null || !fecha.equals(lastDate)) {
      for (Item item : data) {
        Accion accion = saveIfNotExistsAccion(item.getAccion());
        Moneda moneda = saveIfNotExistMoneda(item.getMoneda());
        Lectura lectura = saveIfNotExistLectura(item.getLectura());
        item.setAccion(accion);
        item.setMoneda(moneda);
        item.setLectura(lectura);
      }
      itemRepository.saveAll(data);
//    }
  }

  public LocalDateTime getLastDate() {
    Page<Lectura> lecturas = lecturaRepository
        .findAll(PageRequest.of(0, 1, Direction.ASC, "fecha"));
    List<Lectura> content = lecturas.getContent();
    return content.isEmpty() ? null : content.get(0).getFecha();
  }

  @Transactional
  public Lectura saveIfNotExistLectura(Lectura lectura) {
    logger.debug("Lectura to save: " + lectura);
    Lectura existing = lecturaRepository.findByFecha(lectura.getFecha());
    if (existing != null) {
      return existing;
    }
    lecturaRepository.save(lectura);
    return lectura;
  }

  @Transactional
  public Moneda saveIfNotExistMoneda(Moneda moneda) {
    Moneda existing = monedaRepository.findByNombre(moneda.getNombre());
    if (existing != null) {
      return existing;
    }
    monedaRepository.save(moneda);
    return moneda;
  }

  @Transactional
  public Sector saveIfNotExistSector(Sector sector) {
    Sector existing = sectorRepository.findByNombre(sector.getNombre());
    if (existing != null) {
      return existing;
    }
    sectorRepository.save(sector);
    return sector;
  }

  @Transactional
  public Accion saveIfNotExistsAccion(Accion accion) {
    Accion existing = accionRepository.findByNemonico(accion.getNemonico());
    if (existing != null) {
      return existing;
    }
		Sector sector = saveIfNotExistSector(accion.getSector());
		accion.setSector(sector);
    accionRepository.save(accion);
    return accion;
  }

  @Transactional
  public void exportar(LocalDateTime desde, LocalDateTime hasta) {
    BvlExporter exporterDay = new BvlExporter(xlsPath);
    BvlExporter exporterMonth = new BvlExporter(xlsPath);

    List<LocalDateTime> dates = datesEntre(desde, hasta);

    logger.debug("Exportando para las fechas: " + dates);

    for (LocalDateTime ldt : dates) {
      // LocalDateTime ldt = LocalDateTime.of(ldt, LocalTime.MIN);
      List<Item> data = getItems(ldt, ldt);
      logger.debug("Data " + ldt + " -> " + data);
      Map<LocalDateTime, List<Item>> data2 = new TreeMap<LocalDateTime, List<Item>>();
      if (data != null && !data.isEmpty()) {
        for (Item v : data) {
          LocalDateTime key = v.getFechaLectura();
          if (!data2.containsKey(key)) {
            data2.put(key, new ArrayList<Item>());
          }
          data2.get(key).add(v);
        }
      }
      LocalDateTime k = null;
      List<Item> d2 = null;
      for (LocalDateTime key : data2.keySet()) {
        try {
          ZonedDateTime zdt = key.atZone(ZoneId.systemDefault());
          exporterDay.createXLSDay(Date.from(zdt.toInstant()));
          exporterDay.writeData(d2 = data2.get(k = key));
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
      if (k == null || d2 == null) {
        continue;
      }
      try {
        ZonedDateTime zdt = k.atZone(ZoneId.systemDefault());
        exporterMonth.createXlsMonth(Date.from(zdt.toInstant()));
        exporterMonth.writeData(d2);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
    exporterDay.closeResources();
    exporterMonth.closeResources();
  }

  public List<LocalDateTime> datesEntre(LocalDateTime fecha1, LocalDateTime fecha2) {
    List<LocalDateTime> data = new ArrayList<LocalDateTime>();
    LocalDateTime date = fecha1;
    do {
      data.add(date);
      date = date.plusDays(1);
    } while (fecha2.isAfter(date));
    return data;
  }
}
