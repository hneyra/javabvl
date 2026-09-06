# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

El dominio y el código están en español (`Accion`, `Lectura`, `Moneda`, `Sector`). Mantén esa nomenclatura.

## Qué es

App de escritorio Swing sobre Spring Boot. Sondea la API de la Bolsa de Valores de Lima
(`dataondemand.bvl.com.pe`), guarda cada lectura en H2, exporta XLS (uno por día y otro acumulado por mes) y avisa
por el system tray de las acciones que varían más de un umbral. Se despliega en Windows como jar, con
`deploy/ejecutar.bat`.

## Comandos

Java 25, Spring Boot 4.1.1. Usa el wrapper; no hace falta tener `mvn` instalado.

```bash
./mvnw clean package                          # jar ejecutable en target/
./mvnw spring-boot:run                        # abre la ventana Swing
./mvnw test                                   # toda la suite
./mvnw test -Dtest=BvlServiceIntegrationTest#getLastDateDevuelveLaMasAntigua
java -jar target/bvl-4.6.0.jar --spring.config.location=deploy/bvl.properties
```

Tres cosas que hacen perder tiempo:

- El jar exige **JRE 25+** en la máquina de despliegue.
- La app **no arranca sin display** (`headless(false)` + `JFrame`). Los tests sí son headless.
- `xlsPath` por defecto es una ruta Windows (`E:/tmp/XLS2/`). En macOS/Linux hay que sobreescribirla.

## Configuración

| Propiedad | Uso |
|---|---|
| `baseUrl`, `urlCotizaciones`, `urlHora` | endpoints BVL |
| `xlsPath` | raíz donde `BvlExporter` crea los XLS |
| `alarma` | umbral de variación % que dispara la alerta |
| `horaInicio`, `horaFin`, `intervalo` | cron y ventana de sondeo; `intervalo` debe ser minutos enteros divisores de 60 |
| `alertaTimeout` | cuánto aguanta abierta la alerta antes de cerrarse sola; si falta, 10 min |
| `spring.datasource.url` | H2 en fichero, `ddl-auto=update` |

Dos cosas no obvias:

- `--spring.config.location` **sustituye** al `application.properties` empaquetado, no lo complementa. Toda propiedad
  sin valor por defecto debe existir en `deploy/bvl.properties`; `DeployPropertiesTest` lo verifica en el build.
- El sondeo corre **de lunes a viernes**. El cron cubre el rango de horas completo, así que dispara también antes de
  `horaInicio`; esos disparos los descarta la ventana de `HorarioSondeo`.

## Arquitectura

```
BvlScheduler (cron de las properties, arrancado desde el botón de JBVL)
  └─ ventana horaria: descarta disparos fuera de [horaInicio, horaFin]
       └─ BVL2.process()
            ├─ BvlReader.readData()   → POST urlCotizaciones → BvlItem → List<Item>
            ├─ BvlReader.getFecha()   → GET urlHora → timestamp de la lectura
            ├─ BvlService.saveData()  → dedup de Accion/Moneda/Sector/Lectura + saveAll
            └─ BvlService.exportar()  → BvlExporter → XlsWriter (POI)
       └─ SondeoListener → JBVL: getVariaciones() → TrayIcon + diálogo en el EDT
```

- **`HorarioSondeo`** — properties → cron + ventana. Valida al construir: una config mala impide arrancar en vez de
  fallar a media sesión.
- **`BvlScheduler`** — `CronTrigger` sobre un `TaskScheduler` de un solo hilo. Nunca propaga excepciones: el
  planificador cancelaría la tarea hasta el siguiente reinicio.
- **`JBVL`** — única UI. Enter en los campos de horario reprograma en caliente; si lo tecleado no vale, el error va a
  la barra de estado y se conserva el horario anterior. La alerta se cierra sola y solo hay una en pantalla.
- **`BvlReader`** — `WebClient` bloqueado con `.block()`. En `@PostConstruct` **desactiva la validación TLS de toda la
  JVM**; es deliberado para el endpoint de la BVL.
- **`BvlService`** — persistencia y exportación. Los `saveIfNotExist*` deduplican por clave natural (`nemonico`,
  `nombre`, `fecha`) porque las entidades llegan sin id del reader.
- **`BvlExporter` / `XlsWriter`** — POI sobre `res/template.xls`. Diario: `<xlsPath>/<año>/<Mes>/<yyyy.MM.dd>.xls`,
  una hoja por hora. Mensual: `<xlsPath>/<año>/<yyyy.MM_Mes>.xls`, una hoja por día.
- **`BvlController`** — solo lectura, un `findAll` por entidad. Para inspeccionar la BD sin abrir H2.

### Modelo de datos

`Lectura` es el eje temporal: una fila por instante de sondeo. Cada `Item` es la cotización de una `Accion` en una
`Lectura`. Todo es `@ManyToOne` LAZY con `cascade = REFRESH`, así que las entidades relacionadas **deben guardarse
antes** que el `Item`; eso hace `saveData`.

## Tests

84 tests. No tocan la red, ni la BD de desarrollo, ni abren ventanas.

- Nombra las clases `*Test` o `*IntegrationTest`, **nunca `*IT`**: surefire no recoge ese patrón y el test quedaría
  fuera de `./mvnw test` sin avisar.
- Los `@DataJpaTest` necesitan `@ContextConfiguration(classes = TestJpaConfig.class)`. Sin eso Spring encuentra
  `JavaBvlApplication`, cuyo bean `frame()` exige `BVL2` y `BvlScheduler`, y el contexto no arranca.
- `BvlReaderIntegrationTest` no llama a `reader.init()` a propósito: desactivaría el TLS de la JVM del test.
- Los tests marcados `CARACTERIZACION:` fijan el comportamiento actual, bugs incluidos. Si arreglas el bug,
  actualiza el test; no lo borres.

## Trampas conocidas (no las arregles sin preguntar)

- `BvlService.getLastDate()` devuelve la lectura **más antigua**, no la última: ordena ASC.
  → `BvlServiceIntegrationTest#getLastDateDevuelveLaMasAntigua`
- `BvlService.getHoras()` lanza `DateTimeException` siempre (`LocalDateTime.from(LocalDate)`). Está muerto.
  → `BvlServiceIntegrationTest#getHorasSiempreFalla`
- Las guardas anti-duplicado están comentadas: cada ciclo reinserta los items sobre la misma `Lectura`.
  → `BvlServiceIntegrationTest#saveDataRepetirLaMismaLecturaDuplicaItems`
- `BvlService.exportar()` vuelca al XLS mensual solo el último grupo horario, no todos.
- `BVL2.getVariaciones()` formatea con el locale de la JVM: coma o punto decimal según la máquina.
- `BvlExporter.closeResources()` está vacía; los `Workbook` no se cierran.
- `BvlReader` lanza `BvlLecturaException` en vez de tragarse los errores de red. **No metas diálogos modales en un
  servicio**: bloquearían todos los sondeos siguientes.

## Release (GitHub Actions)

`.github/workflows/release.yml`, en cada push a `main`: tests → sube el patch del POM y el nombre del jar en
`deploy/ejecutar.bat` → `package` → commit `[skip ci]` + tag `vX.Y.Z` → release.

Adjuntos: el jar, un zip con el jar y todo `deploy/` dentro (listo para descomprimir en Windows), y además cada
fichero de `deploy/` suelto. El workflow no fija nombres: lo que metas en esa carpeta entra solo.

- La versión la lleva el workflow. Para un salto de menor o mayor, edita el POM y deja que siga desde ahí.
- `deploy/ejecutar.bat` fija el nombre del jar a mano; el workflow lo reescribe con `sed` y lo verifica con `grep`.

## Boot 4: paquetes que se movieron

| Boot 3 | Boot 4.1 |
|---|---|
| `o.s.boot.autoconfigure.domain.EntityScan` | `o.s.boot.persistence.autoconfigure.EntityScan` |
| `o.s.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `o.s.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `o.s.boot.test.autoconfigure.orm.jpa.TestEntityManager` | `o.s.boot.jpa.test.autoconfigure.TestEntityManager` |

`spring-boot-starter-test` no arrastra esos dos últimos; `spring-boot-data-jpa-test` está declarado en el POM.
Jackson 3 (`tools.jackson`) es el de serie, pero las anotaciones siguen en `com.fasterxml.jackson.annotation`.

## Código muerto

- `JDisplayData` — se instancia desde `JBVL` pero muestra una ventana vacía.
- `BVL2.main()` — runner heredado que hace `new BVL2()` sin Spring; las dependencias quedan nulas.
- `BVL2.dataAnt` — se rellena en cada ciclo y no se lee nunca.

## Convenciones

- Logging SLF4J con concatenación de strings, no placeholders.
- `README.md` son notas de versión.
