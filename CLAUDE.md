# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Nota: el dominio y gran parte del código están en español (Accion, Lectura, Cotizacion, Moneda, Sector). Mantén esa
> nomenclatura al añadir código nuevo.

## Qué es

Aplicación de escritorio (Swing) arrancada por Spring Boot que sondea periódicamente la API pública de la **Bolsa de
Valores de Lima** (`dataondemand.bvl.com.pe`), persiste cada lectura en una base H2 embebida, exporta hojas Excel (una
por día y una acumulada por mes) y avisa por el *system tray* de las acciones que superan un umbral de variación
porcentual.

Empaquetado como jar ejecutable (`bvl-<version>.jar`) y desplegado en Windows con `deploy/ejecutar.bat`.

## Build y ejecución

El proyecto es Maven (sin wrapper `mvnw`). Java 17 como `java.version` del POM, Spring Boot 3.0.2.

```bash
mvn clean package          # compila + empaqueta el jar ejecutable en target/
mvn compile                # solo compilar
mvn spring-boot:run        # arrancar en desarrollo (abre la ventana Swing)
mvn test                   # tests
mvn test -Dtest=NombreTest#metodo   # un solo test / método
```

Ejecutar el jar con configuración externa (así se despliega en producción):

```bash
java -jar target/bvl-4.6.0.jar --spring.config.location=deploy/bvl.properties
```

### Antes de dar por buenas estas órdenes

- **`mvn` no está instalado en esta máquina** (solo hay un JDK Temurin 25). Instálalo (`brew install maven`) o usa el
  Maven que trae IntelliJ antes de afirmar que algo compila o pasa los tests.
- El JDK presente es 25 mientras el POM apunta a 17; si aparecen fallos raros de plugins/bytecode, ese es el primer
  sospechoso.
- `main()` arranca con `.headless(false)` y `frame()` crea un `JFrame` visible: **la app no arranca sin display**. No la
  lances en un entorno headless ni en CI sin `Xvfb`/equivalente.
- No hay tests reales: `src/test/java/bvl/JavaBvlApplicationTests.java` está íntegramente comentado. `mvn test` pasa
  porque no ejecuta nada.
- `xlsPath` en `application.properties` es una ruta Windows (`E:/tmp/XLS2/`). Para probar en macOS/Linux hay que
  sobreescribirla o la exportación fallará.

## Configuración

Todos los ajustes funcionales viven en properties, no en código:

| Propiedad                               | Uso                                                                                                       |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `baseUrl`, `urlCotizaciones`, `urlHora` | endpoints BVL; los inyecta `WebClientConfiguration` y `BvlReader`                                         |
| `xlsPath`                               | raíz donde `BvlExporter` crea `<año>/<Mes>/<fecha>.xls`                                                   |
| `alarma`                                | umbral de variación % que dispara la alerta (valor inicial del campo de la UI)                            |
| `horaInicio`, `intervalo`               | valores iniciales de los campos de la UI; `intervalo` (`hh:mm:ss`) es el periodo real del bucle de sondeo |
| `spring.datasource.url`                 | H2 en fichero (`jdbc:h2:file:...`), `ddl-auto=update`                                                     |

`horaFin` está declarada en `application.properties` pero **no la lee nadie**: el sondeo no se detiene por hora.

`deploy/bvl.properties` es el fichero de producción y solo redefine un subconjunto; se pasa con
`--spring.config.location`, que **sustituye** (no complementa) al `application.properties` empaquetado, así que toda
propiedad usada por el código debe existir allí.

## Arquitectura

Flujo de un ciclo completo (`BVL2.process()`):

```
JBVL (Swing, botón "Iniciar")
  └─ hilo propio, bucle infinito con sleep(intervalo)
       └─ BVL2.process()
            ├─ BvlService.getLastDate()      → fecha de referencia en BD
            ├─ BvlReader.readData()          → POST urlCotizaciones → StockMarket/BvlItem → List<Item>
            ├─ BvlReader.getFecha()          → GET urlHora → Daily.updatedDate (timestamp de la lectura)
            ├─ BvlService.saveData(...)      → dedup de Accion/Moneda/Sector/Lectura + itemRepository.saveAll
            └─ BvlService.exportar(fecha,fecha) → BvlExporter → XlsWriter (POI HSSF)
       └─ BVL2.getVariaciones(umbral) → texto para TrayIcon + JOptionPane
```

Piezas y sus responsabilidades:

- **`JavaBvlApplication`** — entrypoint. Declara el `JBVL` como `@Bean`, por eso la ventana existe dentro del contexto
  de Spring y puede recibir `@Value`.
- **`JBVL`** — única UI viva. Contiene el bucle de sondeo (hilo anónimo dentro del `ActionListener` de "Iniciar"), no
  hay scheduler de Spring.
- **`BVL2`** (`@Service`) — orquestador del ciclo y cálculo del mensaje de alertas. Su `main()` es un runner alternativo
  heredado que instancia `new BVL2()` sin Spring: **no funciona** (las dependencias quedan nulas), ignóralo.
- **`BvlReader`** (`@Service`) — cliente HTTP con `WebClient` reactivo bloqueado con `.block()`. Mapea `BvlItem` (DTO de
  la API) a `Item` (entidad JPA), creando de paso `Accion`/`Sector`/`Moneda`/`Lectura` transitorias. En `@PostConstruct`
  **desactiva la verificación de certificados TLS** globalmente (`disableSSLCertificateChecking`); es deliberado para el
  endpoint de la BVL, tenlo presente antes de tocarlo.
- **`BvlService`** (`@Service`) — toda la persistencia y la orquestación de la exportación. Los métodos
  `saveIfNotExist*` implementan el dedup por clave natural (`nemonico`, `nombre`, `fecha`) porque las entidades llegan
  sin id desde el reader.
- **`BvlExporter` / `XlsWriter`** — POI sobre `src/main/resources/res/template.xls`. `BvlExporter` no es un bean:
  `BvlService.exportar` instancia dos (uno para el fichero diario, otro para el mensual). `createXLSDay` genera
  `<xlsPath>/<año>/<Mes>/<yyyy.MM.dd>.xls` con una hoja por hora (`HH.mm.ss`); `createXlsMonth` genera
  `<xlsPath>/<año>/<yyyy.MM_Mes>.xls` con una hoja por día.
- **`BvlController`** (`@RestController`, puerto 8089) — solo lectura, un `findAll` por entidad (`/item`, `/accion`,
  `/lectura`, `/moneda`). Útil para inspeccionar la BD sin abrir H2.

### Modelo de datos

`Lectura` es el eje temporal: una fila por instante de sondeo (`fecha` único). Cada `Item` es la cotización de una
`Accion` en una `Lectura` concreta, con `Moneda` asociada; `Accion` cuelga de un `Sector`. Todas las relaciones son
`@OneToOne` LAZY con `cascade = REFRESH`, así que las entidades relacionadas **deben guardarse antes** que el `Item` —
eso es lo que hace `saveData`.

`Item.fechaLectura` duplica `item.getLectura().getFecha()`; la exportación agrupa por `fechaLectura`.

## Trampas conocidas (no las "arregles" sin preguntar)

- **`BvlService.getLastDate()` devuelve la lectura MÁS ANTIGUA**, no la última: ordena `Direction.ASC` y toma la primera
  página. `BVL2.process()` la usa para cargar `dataAnt`, que además nunca se lee.
- **Las guardas anti-duplicado están comentadas** en `BVL2.process()` (`if (date != null && lastDate.isBefore(date))`) y
  en `BvlService.saveData` (`if (lastDate == null || !fecha.equals(lastDate))`). Consecuencia: cada ciclo reescribe
  items aunque la BVL no haya publicado datos nuevos, colgándolos de la misma `Lectura`. Está así a propósito en el
  commit actual.
- **`BvlService.exportar`** exporta al fichero mensual solo el último grupo horario del bucle (`k`/`d2` conservan la
  última iteración), no todos.
- **`BvlExporter.closeResources()` está vacía** (cuerpo comentado); los `Workbook`/streams no se cierran explícitamente.
- **`BvlReader.readData()` muestra un `JOptionPane` en caso de error de red** — es código de UI dentro de un servicio;
  cualquier uso no interactivo (test, batch) se quedará bloqueado en el diálogo.

## Código muerto / legado

No lo tomes como referencia y no lo extiendas sin motivo:

- `bvl.bean.*` (`ReadDataBean`, `ReadItemBean`, `LastReadBean`) — `@Deprecated`, DTOs del scraping HTML anterior a la
  API JSON (ver README, versión 4.0.0). Sustituidos por `bvl.domain.input.*` (`StockMarket`, `BvlItem`, `Daily`).
- `JCotizaciones`, `JTableAcciones`, `JFilterDialog`, `ExportDialog`, `bvl.ui.CheckBoxNodeTreeSample` — pantallas Swing
  no cableadas; sus únicos puntos de entrada están comentados. `JDisplayData` sí se instancia desde `JBVL` pero muestra
  una ventana vacía.
- `BvlReader.extractJson`, `getFechaInicio`, `getHoraInicio`, `parseLocalDate/Long/Double`, `forceTrim` — parseo del
  HTML antiguo, sin llamadas.

## Convenciones
- Logging con SLF4J (`private final Logger logger = LoggerFactory.getLogger(getClass());`) y concatenación de strings,
  no placeholders. `logging.level.bvl=DEBUG` en desarrollo.
- El `README.md` son notas de versión; actualízalo al subir `<version>` en el POM (y recuerda que el nombre del jar en
  `deploy/ejecutar.bat` está fijado a mano).
