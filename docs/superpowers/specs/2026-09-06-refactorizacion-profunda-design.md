# Refactorización profunda de JavaBVL

**Fecha:** 2026-09-06
**Estado:** aprobado
**Línea base:** 89 tests verdes, `BUILD SUCCESS`, versión 4.6.2

## Objetivo

Reestructurar la aplicación para que sea legible, mantenible y extensible, **sin perder
funcionalidad y sin cambiar comportamiento observable**. La suite existente es el contrato: los
89 tests, incluidos los de CARACTERIZACIÓN que fijan bugs conocidos, deben seguir verdes.

## Decisiones tomadas

| Decisión | Elección |
|---|---|
| Trampas conocidas de CLAUDE.md | **Refactor puro**: los 6 bugs se conservan intactos, aislados y documentados |
| Alcance | **Reestructuración completa**: paquetes, nombres de clase, inyección por constructor |
| Nomenclatura | Dominio **en español**; logging pasa a **placeholders `{}`** de SLF4J |
| Código muerto | Se borra el interno; se conservan los controles visibles de la UI |
| Módulos Maven | No. Sigue siendo un único artefacto: el workflow de release lo asume |

## Restricciones duras

1. **Ningún nombre de propiedad cambia.** `deploy/bvl.properties` y `deploy/ejecutar.bat` siguen
   siendo válidos sin tocarlos. `--spring.config.location` sustituye al properties empaquetado, así
   que cualquier propiedad nueva sin default rompería producción y solo producción.
2. **El mapeo JPA no cambia.** Mismas tablas, mismas columnas, misma `ddl-auto=update` sobre bases
   H2 ya existentes en máquinas de despliegue.
3. **Los ficheros XLS generados no cambian**: rutas, nombres de fichero, nombres de hoja, posición
   de cada columna y contenido de las cabeceras.
4. **El formato de los mensajes de alerta no cambia**, carácter por carácter.
5. La app sigue sin arrancar headless (`headless(false)` + `JFrame`); los tests siguen headless.

## Estructura destino

Paquetes en inglés (como hoy), dominio en español (como manda CLAUDE.md).

```
bvl/
├─ JavaBvlApplication
├─ config/      BvlProperties · WebClientConfiguration · SchedulingConfiguration · SwingConfiguration
├─ domain/      Accion · Item · Lectura · Moneda · Sector
├─ market/      BvlClient · CotizacionMapper · LectorBvl · BvlLecturaException
│  └─ dto/      BvlItem · Daily · StockMarket          (records)
├─ repository/  Accion/Item/Lectura/Moneda/SectorRepository
├─ service/     CatalogoService · LecturaService · ExportService
├─ export/      BvlExporter · XlsWriter · CabeceraCotizaciones · EstilosXls · RutaXls · PlantillaXls
├─ alert/       DetectorVariaciones · Variacion · AlertaFormatter
├─ schedule/    CicloSondeo · ResultadoSondeo · BvlScheduler · HorarioSondeo · SondeoListener
├─ ui/          VentanaPrincipal · PanelSondeo · BandejaSistema · AlertaDialogo · VentanaDatos
└─ controller/  BvlController
```

## Descomposición

### `BVL2` se disuelve

Nombre sin significado, estado mutable compartido entre el hilo del planificador y el EDT, y
generación de HTML dentro de un `@Service`.

- `schedule/CicloSondeo.process()` → devuelve `ResultadoSondeo(List<Item> items, LocalDateTime fecha)`
  en vez de dejarlo en un campo. Elimina la carrera entre hilos.
- `alert/DetectorVariaciones.detectar(items, umbral) → List<Variacion>` — lógica pura.
- `alert/AlertaFormatter` — texto del tray y HTML del diálogo, idénticos a los actuales.

Se borra `BVL2.main()` (runner huérfano sin Spring) y `BVL2.dataAnt` (se rellena, nunca se lee).

### `BvlService` se parte en tres

| Nuevo | Contenido |
|---|---|
| `CatalogoService` | los cuatro `saveIfNotExist*`, dedup por clave natural |
| `LecturaService` | `saveData`, `getItems` ×2, `getLastDate`, `getHoras` |
| `ExportService` | `exportar`, `datesEntre` |

Se borra `prepareValues` (construye SQL a mano, nadie lo llama). Los tres `SimpleDateFormat`
**públicos, estáticos y mutables** —compartidos entre hilos y usados desde `BvlExporter`, que por eso
depende de `service`— se sustituyen por `DateTimeFormatter` inmutables en `export/RutaXls`. Eso
rompe la dependencia invertida `export → service`.

`getHoras` **se conserva**: está muerto pero lo fija un test de caracterización.

### `JBVL` (421 líneas) se parte en cuatro

- `VentanaPrincipal` — `JFrame`, compone y cablea.
- `PanelSondeo` — los cuatro campos y los tres botones.
- `BandejaSistema` — `SystemTray` tras una interfaz `Notificador`, con implementación no-op cuando
  el tray no está soportado (sustituye a los `trayIcon != null` repartidos).
- `AlertaDialogo` — diálogo modal con autocierre y descarte de la alerta anterior.

Se conservan el botón **Mostrar** con su ventana y el botón **Exportar** sin efecto: son visibles.

### `BvlReader` se parte en dos

- `BvlClient` — HTTP puro, devuelve `StockMarket` y `Daily`.
- `CotizacionMapper` — `BvlItem` → `Item`.
- `LectorBvl` — fachada `readData()` / `getFecha()`, conserva el orden actual de llamadas
  (`getFecha()` antes del POST).

El desactivado global de TLS de la JVM se aísla con Javadoc explicando que es deliberado.

### Exportación

- `BvlExporter.createSheet`, 90 líneas de llamadas POI en cascada, pasa a una **tabla declarativa**
  de columnas (título, ancho, color, fusión) más un bucle corto.
- `XlsWriter.useStyle2/useStyle3/useStyleAlign` → `estiloCabecera()`, `fondoYBordes(color)`,
  `centradoConAjuste()`.
- `cargarPlantilla`: bucle byte a byte con `finally` anidados → `try-with-resources` + `transferTo`.
- Nombres de fichero: concatenación con relleno manual de ceros → `DateTimeFormatter`. Salida
  idéntica.
- `closeResources()` **sigue vacía**: bug preservado, ahora documentado.
- Se borran `XlsWriter.main()` (apunta a `F:\workbook.xls`) y `useDefaultStyle()` (cuerpo vacío).

### Configuración

Los nombres de propiedad, hoy repartidos por `@Value` sobre campos en 6 clases, se concentran en un
único `BvlProperties` inyectado por constructor, con tipos reales (`Duration`, `Path`,
`HorarioSondeo`, `double`, `URI`) validados al arrancar. **Ningún nombre de propiedad cambia.**

### Transversal

Inyección por constructor · los 8 `printStackTrace` pasan a SLF4J · logging con placeholders ·
DTOs de entrada a `record` · cadenas de `instanceof` a `switch` con patrones.

## El único cambio de comportamiento

Hoy, al terminar un sondeo, la UI llama a `bvl.getFecha()`, que dispara **una segunda petición
HTTP** solo para el título del globo del tray. Al viajar la fecha dentro de `ResultadoSondeo` esa
petición desaparece: mismo endpoint, mismo valor, una llamada de red menos y una `BvlLecturaException`
menos que puede saltar al presentar la alerta.

## Bugs preservados (con su test de caracterización)

| Bug | Dónde queda | Test |
|---|---|---|
| `getLastDate()` devuelve la lectura más antigua | `LecturaService` | `getLastDateDevuelveLaMasAntigua` |
| `getHoras()` siempre lanza `DateTimeException` | `LecturaService` | `getHorasSiempreFalla` |
| Cada ciclo reinserta los items | `LecturaService.saveData` | `saveDataRepetirLaMismaLecturaDuplicaItems` |
| El XLS mensual solo vuelca el último grupo horario | `ExportService.exportar` | — |
| El formato de variaciones depende del locale | `AlertaFormatter` | `BVL2GetVariacionesTest` fija `Locale.US` |
| Los `Workbook` no se cierran | `BvlExporter.closeResources` | — |

Cada uno lleva Javadoc `TRAMPA CONOCIDA:` en su nuevo emplazamiento.

## Plan de ejecución

Ocho fases, `./mvnw test` verde al final de cada una.

1. **Config** — `BvlProperties`, inyección por constructor en las clases que ya existen.
2. **Market** — `BvlReader` → `BvlClient` + `CotizacionMapper` + `LectorBvl`; DTOs a records.
3. **Services** — `BvlService` → `CatalogoService` + `LecturaService` + `ExportService`.
4. **Export** — `RutaXls`, `EstilosXls`, `CabeceraCotizaciones`, `PlantillaXls`, `XlsWriter` renombrado.
5. **Alert + ciclo** — `DetectorVariaciones`, `AlertaFormatter`, `CicloSondeo`, `ResultadoSondeo`;
   `SondeoListener` pasa a recibir el resultado.
6. **UI** — `JBVL` → `VentanaPrincipal` + `PanelSondeo` + `BandejaSistema` + `AlertaDialogo`.
7. **Limpieza** — borrado del código muerto, `printStackTrace`, logging, imports comodín.
8. **Cierre** — `./mvnw clean package`, actualización de `CLAUDE.md`.

## Verificación

- `./mvnw test` ≥ 89 tests verdes tras cada fase.
- `./mvnw clean package` produce el jar al final.
- `DeployPropertiesTest` blinda el contrato de propiedades.
- Los tests se mueven con su código conservando **todas** las aserciones; los de CARACTERIZACIÓN no
  cambian ni una.

**Fuera de alcance de la verificación automática:** que la ventana Swing se pinte igual. La app no
arranca sin display. Se verifica compilación y cableado; el aspecto visual lo confirma el usuario
con `./mvnw spring-boot:run`.
