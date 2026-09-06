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

Java 25 como `java.version` del POM, Spring Boot 4.1.1. **Usa siempre el wrapper** (`./mvnw`, `mvnw.cmd` en Windows):
está en el repo y fija Maven 3.9.11, así que no hace falta tener `mvn` instalado.

```bash
./mvnw clean package        # compila + empaqueta el jar ejecutable en target/
./mvnw compile              # solo compilar
./mvnw spring-boot:run      # arrancar en desarrollo (abre la ventana Swing)
./mvnw test                 # toda la suite
./mvnw test -Dtest=BvlServiceIntegrationTest              # una clase
./mvnw test -Dtest=BvlServiceIntegrationTest#getLastDateDevuelveLaMasAntigua   # un método
```

Ejecutar el jar con configuración externa (así se despliega en producción):

```bash
java -jar target/bvl-4.6.0.jar --spring.config.location=deploy/bvl.properties
```

### Antes de dar por buenas estas órdenes

- El POM compila con `release 25` y la máquina tiene Temurin 25, así que build y despliegue van a la par. **El jar
  exige un JRE 25+**: si el equipo Windows donde corre `deploy/ejecutar.bat` tiene uno anterior, no arranca.
- `main()` arranca con `.headless(false)` y `frame()` crea un `JFrame` visible: **la app no arranca sin display**. No la
  lances en un entorno headless ni en CI sin `Xvfb`/equivalente. Los tests sí son headless-safe (ver más abajo).
- `xlsPath` en `application.properties` es una ruta Windows (`E:/tmp/XLS2/`). Para probar en macOS/Linux hay que
  sobreescribirla o la exportación fallará.

## Configuración

Todos los ajustes funcionales viven en properties, no en código:

| Propiedad                               | Uso                                                                                                       |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `baseUrl`, `urlCotizaciones`, `urlHora` | endpoints BVL; los inyecta `WebClientConfiguration` y `BvlReader`                                         |
| `xlsPath`                               | raíz donde `BvlExporter` crea `<año>/<Mes>/<fecha>.xls`                                                   |
| `alarma`                                | umbral de variación % que dispara la alerta (valor inicial del campo de la UI)                            |
| `horaInicio`, `horaFin`, `intervalo`    | definen el cron y la ventana de sondeo (ver `HorarioSondeo`); `intervalo` (`hh:mm:ss`) debe ser minutos enteros divisores de 60 |
| `spring.datasource.url`                 | H2 en fichero (`jdbc:h2:file:...`), `ddl-auto=update`                                                     |

El sondeo corre **de lunes a viernes** dentro de `[horaInicio, horaFin]`. El cron cubre la rejilla del reloj para el rango de horas completo, así que dispara también antes de `horaInicio`; esos disparos los descarta la guarda de ventana de `HorarioSondeo`.

`deploy/bvl.properties` es el fichero de producción y solo redefine un subconjunto; se pasa con
`--spring.config.location`, que **sustituye** (no complementa) al `application.properties` empaquetado, así que toda
propiedad usada por el código debe existir allí.

## Arquitectura

Flujo de un ciclo completo (`BVL2.process()`):

```
BvlScheduler (cron derivado de las properties, arrancado desde el botón de JBVL)
  └─ guarda de ventana: descarta los disparos fuera de [horaInicio, horaFin]
       └─ BVL2.process()
            ├─ BvlService.getLastDate()      → fecha de referencia en BD
            ├─ BvlReader.readData()          → POST urlCotizaciones → StockMarket/BvlItem → List<Item>
            ├─ BvlReader.getFecha()          → GET urlHora → Daily.updatedDate (timestamp de la lectura)
            ├─ BvlService.saveData(...)      → dedup de Accion/Moneda/Sector/Lectura + itemRepository.saveAll
            └─ BvlService.exportar(fecha,fecha) → BvlExporter → XlsWriter (POI HSSF)
       └─ SondeoListener → JBVL: getVariaciones(umbral) → TrayIcon + diálogo en el EDT
```

Piezas y sus responsabilidades:

- **`JavaBvlApplication`** — entrypoint. Declara el `JBVL` como `@Bean`, por eso la ventana existe dentro del contexto
  de Spring y puede recibir `@Value`.
- **`bvl.schedule.HorarioSondeo`** — lógica pura que traduce `horaInicio`/`horaFin`/`intervalo` a expresión cron y
  ventana horaria. Valida al construir: un properties mal puesto **impide arrancar** en vez de fallar a media sesión.
- **`bvl.schedule.BvlScheduler`** (`@Service`) — programa el sondeo con un `CronTrigger` sobre el `TaskScheduler` de un
  solo hilo que declara `SchedulingConfiguration`. `iniciar()`/`detener()` lo gobiernan desde la UI. Nunca deja escapar
  una excepción: si lo hiciera, el planificador cancelaría la tarea hasta el siguiente reinicio.
- **`JBVL`** — única UI viva. El toggle Iniciar/Detener manda sobre el scheduler; la ventana se registra como
  `SondeoListener` y pinta el resultado en el EDT, sin bloquear el hilo del planificador.
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
`@ManyToOne` LAZY con `cascade = REFRESH`, así que las entidades relacionadas **deben guardarse antes** que el `Item` —
eso es lo que hace `saveData`.

`Item.fechaLectura` duplica `item.getLectura().getFecha()`; la exportación agrupa por `fechaLectura`.

## Tests

75 tests en 10 clases, todos en `./mvnw test`. No tocan la red, ni la BD de desarrollo, ni abren ventanas: surefire
fuerza `java.awt.headless=true` desde el `pom.xml` y `src/test/resources/application.properties` apunta a una H2 en
memoria.

| Clase | Cubre |
|---|---|
| `HorarioSondeoTest` | cron y ventana horaria a partir de las properties, con sus validaciones |
| `BvlSchedulerTest` | guarda de ventana, alta/baja de la tarea, aislamiento de fallos |
| `BvlSchedulerWiringTest` | cableado real con Spring: properties, `TaskScheduler` y fallo al arrancar |
| `BvlServiceDatesEntreTest` | `datesEntre`, sin Spring |
| `BVL2GetVariacionesTest` | mensaje de alertas: umbral, signo, redacción, nulos |
| `XlsWriterTest` | escritura de tipos, filas y hojas, releyendo con POI |
| `RepositoriesIntegrationTest` | métodos derivados de los repositorios contra H2 |
| `BvlServiceIntegrationTest` | deduplicación de catálogos y consultas por lectura |
| `BvlReaderIntegrationTest` | contrato con la API BVL y mapeo `BvlItem`→`Item`, con `WebClient` stub |
| `BvlExporterIntegrationTest` | layout de carpetas/hojas y posición de cada columna del XLS |

Convenciones que conviene respetar al añadir tests:

- **Nombra las clases `*Test` o `*IntegrationTest`, nunca `*IT`.** Surefire no recoge el patrón `*IT.java` (ese es de
  failsafe, y el proyecto no lo configura), así que el test quedaría fuera de `./mvnw test` sin avisar de nada.
- Las clases `@Nested` sí funcionan con el surefire 3.5.6 que trae Boot 4.1 (no era el caso con el 2.22.2 de Boot 3).
  Las clases actuales están planas por herencia de la versión anterior; no hay que aplanar las nuevas.
- Los tests de persistencia usan `@DataJpaTest` + `@ContextConfiguration(classes = TestJpaConfig.class)`.
  `TestJpaConfig` (en `bvl.support`) es **obligatoria**: sin ella Spring encuentra `JavaBvlApplication`, cuyo bean
  `frame()` construye un `JFrame` y exige `BVL2` y `BvlScheduler`; un contexto recortado a JPA no tiene ninguno de los
  dos y no arranca.
- `BvlReaderIntegrationTest` **no llama a `reader.init()`** a propósito: ese `@PostConstruct` desactiva la validación de
  certificados TLS de toda la JVM.
- Los tests marcados `CARACTERIZACION:` fijan el comportamiento actual, incluido el que parece un bug. Están enlazados
  con la sección siguiente. Si arreglas alguna de esas trampas, el test que la caracteriza **debe** fallar: actualízalo,
  no lo borres.

## Trampas conocidas (no las "arregles" sin preguntar)

- **`BvlService.getLastDate()` devuelve la lectura MÁS ANTIGUA**, no la última: ordena `Direction.ASC` y toma la primera
  página. `BVL2.process()` la usa para cargar `dataAnt`, que además nunca se lee.
  → `BvlServiceIntegrationTest#getLastDateDevuelveLaMasAntigua`
- **`BvlService.getHoras()` lanza `DateTimeException` con cualquier entrada**: `LocalDateTime.from(fecha.toLocalDate())`
  no puede construir una hora a partir de un `LocalDate`, que no tiene campos de tiempo. El método está muerto en
  producción (no lo llama nadie), por eso nunca ha dado la cara.
  → `BvlServiceIntegrationTest#getHorasSiempreFalla`
- **Las guardas anti-duplicado están comentadas** en `BVL2.process()` (`if (date != null && lastDate.isBefore(date))`) y
  en `BvlService.saveData` (`if (lastDate == null || !fecha.equals(lastDate))`). Consecuencia: cada ciclo reescribe
  items aunque la BVL no haya publicado datos nuevos, colgándolos de la misma `Lectura`. Está así a propósito en el
  commit actual.
  → `BvlServiceIntegrationTest#saveDataRepetirLaMismaLecturaDuplicaItems`
- **`BvlService.exportar`** exporta al fichero mensual solo el último grupo horario del bucle (`k`/`d2` conservan la
  última iteración), no todos.
- **`BVL2.getVariaciones` formatea con el locale por defecto de la JVM**: en una máquina `de_DE` el mensaje sale con
  coma decimal (`subió 3,5%`) y en `en_US` con punto. El test fija el locale para no depender de la máquina.
- **`BvlExporter.closeResources()` está vacía** (cuerpo comentado); los `Workbook`/streams no se cierran explícitamente.
- **`BvlReader` ya no traga los errores de red**: `readData()` y `getFecha()` lanzan `BvlLecturaException` en lugar de
  devolver lista vacía o `null`. Quien orquesta decide cómo avisar; `BvlScheduler` lo captura, lo registra y lo pasa al
  `SondeoListener`. No vuelvas a meter un diálogo modal en un servicio: bloquearía todos los sondeos siguientes.

## Migración a Spring Boot 4.1 (hecha)

El proyecto saltó de Boot 3.0.2 a 4.1.1 de una vez. Lo que hay que saber para no repetir el trabajo:

- **Paquetes que cambiaron de sitio** en Boot 4, todos ya corregidos. Si ves documentación antigua, estos son los
  nuevos:

  | Antes (Boot 3) | Ahora (Boot 4.1) |
  |---|---|
  | `o.s.boot.autoconfigure.domain.EntityScan` | `o.s.boot.persistence.autoconfigure.EntityScan` |
  | `o.s.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `o.s.boot.data.jpa.test.autoconfigure.DataJpaTest` |
  | `o.s.boot.test.autoconfigure.orm.jpa.TestEntityManager` | `o.s.boot.jpa.test.autoconfigure.TestEntityManager` |

  `@DataJpaTest` y `TestEntityManager` viven ahora en artefactos propios (`spring-boot-data-jpa-test`,
  `spring-boot-jpa-test`) que **`spring-boot-starter-test` no arrastra**: el primero está declarado en el POM, el
  segundo llega como transitiva suya.
- **Jackson 3 es el de serie** (`tools.jackson.*`); `databind` y `datatype-jsr310` cambiaron de paquete. Las
  anotaciones **no**: `@JsonIgnore` y `@JsonFormat` siguen en `com.fasterxml.jackson.annotation`, por eso las entidades
  no se tocaron. El `ObjectMapper` de `BvlReader` era un campo muerto y se eliminó; `WebClient` deserializa con los
  codecs de Spring, no con él.
- **`@OneToOne` → `@ManyToOne` en `Item.lectura`, `Item.accion`, `Item.moneda` y `Accion.sector`.** Hibernate 7 impone
  un `UNIQUE` en la columna de unión de un `@OneToOne` e **ignora el `unique = false`** que estas relaciones traían;
  Hibernate 6.1 lo respetaba. Con el modelo anterior no se puede insertar más de un `Item` por lectura y el arranque
  fallaba en la segunda acción. El DDL resultante vuelve a ser el de antes, así que la BD H2 de producción sigue
  siendo compatible con `ddl-auto=update`.
- **Código muerto eliminado en la misma pasada**: el paquete `bvl.bean.*` (DTOs `@Deprecated` del scraping HTML), las
  pantallas Swing no cableadas (`JCotizaciones`, `JTableAcciones`, `JFilterDialog`, `ExportDialog`,
  `bvl.ui.CheckBoxNodeTreeSample`), los parsers del HTML antiguo en `BvlReader` (`extractJson`, `getFechaInicio`,
  `getHoraInicio`, `parseLocalDate/Long/Double`, `forceTrim`) y la dependencia `jsoup`, que no se usaba desde el
  cambio a la API JSON.

## Código muerto / legado

Lo que queda vivo pero sin uso, no lo tomes como referencia:

- `JDisplayData` se instancia desde `JBVL` pero muestra una ventana vacía: la línea que le ponía contenido está
  comentada desde que se borró `JCotizaciones`.
- `BVL2.main()` es un runner heredado que hace `new BVL2()` sin Spring; las dependencias quedan nulas y no funciona.
- `BVL2.dataAnt` se rellena en cada ciclo y no se lee nunca.

## Convenciones
- Logging con SLF4J (`private final Logger logger = LoggerFactory.getLogger(getClass());`) y concatenación de strings,
  no placeholders. `logging.level.bvl=DEBUG` en desarrollo.
- El `README.md` son notas de versión; actualízalo al subir `<version>` en el POM (y recuerda que el nombre del jar en
  `deploy/ejecutar.bat` está fijado a mano).
