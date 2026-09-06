# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

El dominio y el código están en español (`Accion`, `Lectura`, `Moneda`, `Sector`). Mantén esa nomenclatura.

## Qué es

App de escritorio Swing sobre Spring Boot. Sondea la API de la Bolsa de Valores de Lima
(`dataondemand.bvl.com.pe`), exporta cada lectura a XLS (uno por día y otro acumulado por mes) y avisa por el
system tray de las acciones que varían más de un umbral. Se despliega en Windows como jar, con
`deploy/ejecutar.bat`.

**No hay base de datos.** Las cotizaciones van de la BVL al XLS, que es el archivo que consulta el usuario. Hubo
una H2 embebida que se quitó: solo servía de ida y vuelta entre leer y exportar.

## Comandos

Java 25, Spring Boot 4.1.1. Usa el wrapper; no hace falta tener `mvn` instalado.

```bash
./mvnw clean package                          # jar ejecutable en target/
./mvnw spring-boot:run                        # abre la ventana Swing
./mvnw test                                   # toda la suite
./mvnw test -Dtest=ExportServiceIntegrationTest#dosCiclosConLaMismaFechaNoDuplicanFilas
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

Dos cosas no obvias:

- `--spring.config.location` **sustituye** al `application.properties` empaquetado, no lo complementa. Toda propiedad
  sin valor por defecto debe existir en `deploy/bvl.properties`; `DeployPropertiesTest` lo verifica en el build.
- El sondeo corre **de lunes a viernes**. El cron cubre el rango de horas completo, así que dispara también antes de
  `horaInicio`; esos disparos los descarta la ventana de `HorarioSondeo`.

## Arquitectura

Paquetes en inglés, dominio en castellano.

```
bvl/
├─ config/      BvlProperties · WebClientConfiguration · SchedulingConfiguration · SwingConfiguration
├─ domain/      Accion · Item · Moneda · Sector   (objetos planos, sin ORM)
├─ market/      BvlClient · CotizacionMapper · LectorBvl · TlsInseguro · BvlLecturaException
│  └─ dto/      BvlItem · Daily · StockMarket   (records, nombres en inglés = los del JSON)
├─ service/     ExportService
├─ export/      BvlExporter · XlsWriter · CabeceraCotizaciones · ColumnaCotizacion · RutaXls · PlantillaXls
├─ alert/       DetectorVariaciones · Variacion · AlertaFormatter
├─ schedule/    BvlScheduler · HorarioSondeo · CicloSondeo · ResultadoSondeo · SondeoListener
└─ ui/          VentanaPrincipal · PanelSondeo · BandejaSistema · AlertaDialogo · Notificador · VentanaDatos
```

Recorrido de un sondeo:

```
BvlScheduler (arranca al pulsar Iniciar; la app en reposo no procesa nada)
  ├─ sondearAhora()    → una lectura ya, sin mirar la ventana
  └─ cron de las properties
       └─ ventana horaria: descarta disparos fuera de [horaInicio, horaFin]
       └─ CicloSondeo.process() → ResultadoSondeo(items, fecha)
            ├─ LectorBvl.readData()     → BvlClient (HTTP) + CotizacionMapper (JSON → dominio)
            └─ ExportService.exportar() → BvlExporter → XlsWriter (POI), desde memoria
       └─ SondeoListener → VentanaPrincipal (en el EDT):
            DetectorVariaciones → AlertaFormatter → BandejaSistema + AlertaDialogo
```

- **`BvlProperties`** — el único sitio donde aparece el nombre de una propiedad. Inyección por
  constructor en todas partes. Valida el horario al arrancar: una config mala impide levantar la
  app en vez de fallar a media sesión. `alarma` y `alertaTimeout` viajan **en crudo** a propósito
  (la primera se muestra tal cual en el campo; la segunda la parsea la UI para poder degradar a 10
  min en vez de tumbar el arranque).
- **`HorarioSondeo`** — properties → cron + ventana. El cron cubre el rango de horas completo, así
  que dispara también antes de `horaInicio`; esos disparos los descarta la ventana.
- **`BvlScheduler`** — `CronTrigger` sobre un `TaskScheduler` de un solo hilo. Al pulsar Iniciar
  hace una **lectura inmediata** (`sondearAhora()`, que **ignora la ventana** a propósito) y además
  programa el cron. Nunca propaga excepciones: el planificador cancelaría la tarea hasta el
  siguiente reinicio.
- **`CicloSondeo`** — el recorrido completo de una lectura, y nada más. No sabe de horarios ni de UI.
  Devuelve `ResultadoSondeo` en vez de dejar las cotizaciones en un campo compartido entre hilos.
- **`LectorBvl`** — única frontera con la BVL y **único sitio que envuelve errores** en
  `BvlLecturaException`. `BvlClient` es transporte puro (`.block()`), `CotizacionMapper` traduce.
  **No metas diálogos modales aquí**: corre en el hilo del planificador y bloquearía todos los
  sondeos siguientes.
- **`TlsInseguro`** — desactiva la validación TLS de `HttpsURLConnection` en toda la JVM. Deliberado
  para el endpoint de la BVL; en su propia clase para que se vea y para que los tests no lo
  arrastren.
- **`ExportService`** — vuelca al XLS **lo que se acaba de leer**, sin pasar por disco intermedio.
- **`ColumnaCotizacion`** — el orden de las columnas del XLS, compartido por la cabecera y la fila
  de datos para que no puedan desalinearse. **`CabeceraCotizaciones`** declara la maqueta de las
  tres filas de cabecera; **`RutaXls`** decide nombres de fichero y de hoja.
  Diario: `<xlsPath>/<año>/<Mes>/<yyyy.MM.dd>.xls`, una hoja por hora.
  Mensual: `<xlsPath>/<año>/<yyyy.MM_Mes>.xls`, una hoja por día.
- **`VentanaPrincipal`** — única ventana; solo compone y cablea. Enter en los campos de horario
  reprograma en caliente; si lo tecleado no vale, el error va a la barra de estado y se conserva el
  horario anterior. Todo lo que llega del planificador se despacha al EDT.
- **`SwingConfiguration`** — construye la ventana **solo si hay pantalla**. En producción siempre la
  hay (`headless(false)`); en la suite no, y por eso el contexto completo puede arrancar en los
  tests.

### Modelo de datos

Un `Item` es la cotización de una `Accion` (con su `Sector`) en una `Moneda` y en un instante.
`fechaLectura` es ese instante, lo publica la BVL y lo comparten todos los items de un mismo sondeo:
es lo que agrupa una lectura y da nombre a la hoja del XLS.

**La variación no la calcula la app**: llega de la BVL en `percentageChange`, y es la variación
contra el cierre de la sesión anterior, que también viene en la respuesta (`previous`,
`previousDate`). Por eso no hace falta guardar lecturas para poder alertar.

## Tests

99 tests. No tocan la red, ni el disco del usuario, ni abren ventanas.

- Nombra las clases `*Test` o `*IntegrationTest`, **nunca `*IT`**: surefire no recoge ese patrón y el
  test quedaría fuera de `./mvnw test` sin avisar.
- `ArranqueIntegrationTest` levanta el contexto **entero**. Es el único que lo hace: sin él, un bean
  sin declarar o una propiedad mal escrita no se verían hasta ejecutar el jar en despliegue.
- En un `ApplicationContextRunner`, registra `PropertySourcesPlaceholderConfigurer`. Sin él los
  placeholders se resuelven con `Environment.resolvePlaceholders`, que deja `${loQueFalte}` como
  literal en vez de fallar: el test sería más permisivo que producción.
- `LectorBvlIntegrationTest` no instancia `TlsInseguro` a propósito: desactivaría el TLS de la JVM
  del test.
- Los tests marcados `CARACTERIZACION:` fijan el comportamiento actual, bugs incluidos. Si arreglas
  el bug, actualiza el test; no lo borres.

## Trampas conocidas (no las arregles sin preguntar)

Cada una lleva un Javadoc `TRAMPA CONOCIDA:` en su clase.

- `AlertaFormatter` formatea con el locale de la JVM: coma o punto decimal según la máquina.
- `LectorBvl` pide la fecha **dos veces** por ciclo (`readData()` la pide para sí y `CicloSondeo`
  la vuelve a pedir). Si las dos respuestas no coincidieran, los items llevarían un instante y la
  hoja del XLS otro.
- `BvlExporter.closeResources()` y `XlsWriter.closeResources()` están vacías; los `Workbook` no se
  cierran.

## Release (GitHub Actions)

`.github/workflows/release.yml`, en cada push a `main`: tests → sube el patch del POM y el nombre del jar en
`deploy/ejecutar.bat` → `package` → commit `[skip ci]` + tag `vX.Y.Z` → release.

Adjuntos: el jar, un zip con el jar y todo `deploy/` dentro (listo para descomprimir en Windows), y además cada
fichero de `deploy/` suelto. El workflow no fija nombres: lo que metas en esa carpeta entra solo.

- La versión la lleva el workflow. Para un salto de menor o mayor, edita el POM y deja que siga desde ahí.
- `deploy/ejecutar.bat` fija el nombre del jar a mano; el workflow lo reescribe con `sed` y lo verifica con `grep`.

## Boot 4

Jackson 3 (`tools.jackson`) es el de serie. Los DTO de entrada son `record`, que Jackson deserializa
sin anotaciones porque el POM compila con `-parameters`.

## Controles sin efecto

- `VentanaDatos` — la abre el botón **Mostrar** y está vacía; nunca se le puso contenido.
- El botón **Exportar** tiene el manejador vacío. Los dos se conservan porque el usuario los ve.

## Convenciones

- Logging SLF4J con placeholders `{}`, no concatenación de strings.
- Inyección por constructor, nunca sobre campos.
- `README.md` son notas de versión.
