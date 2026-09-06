# JavaBVL

Aplicación de escritorio que vigila la Bolsa de Valores de Lima por ti.

Cada pocos minutos consulta las cotizaciones, las guarda en una hoja de Excel y te avisa por el
icono de la bandeja del sistema cuando una acción se mueve más de lo que le hayas dicho. La dejas
abierta por la mañana y te olvidas.

[![Licencia: LGPL v3](https://img.shields.io/badge/licencia-LGPL%20v3-blue.svg)](LICENSE)

## Para qué sirve

Si sigues varias acciones de la BVL, la alternativa es tener la web abierta y mirarla cada rato.
Esta aplicación hace eso sola y solo te interrumpe cuando pasa algo:

- **Te avisa de los movimientos que te importan.** Fijas un umbral (por ejemplo, 2 %) y solo
  aparece un aviso cuando alguna acción lo supera.
- **Te deja el histórico en Excel**, listo para tus propias hojas y gráficos, sin copiar y pegar.
- **Se queda en la bandeja del sistema**, sin ocupar sitio en la pantalla.

## Cómo funciona

Al pulsar **Iniciar**, la aplicación hace una lectura inmediata y luego repite cada `intervalo`
minutos, de lunes a viernes, entre `horaInicio` y `horaFin`. En cada lectura:

1. Pide las cotizaciones a la API pública de la BVL (`dataondemand.bvl.com.pe`).
2. Las escribe en dos ficheros Excel:
   - **el del día**, `<xlsPath>/2026/Enero/2026.01.15.xls`, con una hoja por hora de consulta;
   - **el del mes**, `<xlsPath>/2026/2026.01_Enero.xls`, con una hoja por día.
3. Compara y, si algo se ha movido por encima del umbral, te lo muestra.

Hay **dos tipos de aviso**, y salen juntos en el mismo mensaje:

| Aviso | Contra qué compara | Disponible |
|---|---|---|
| Variación del día | el cierre de la sesión anterior | siempre; lo publica la propia BVL |
| Movimiento intradía | la consulta anterior de la aplicación | a partir de la segunda consulta |

El aviso se cierra solo pasado `alertaTimeout` si no estás delante, y nunca se acumulan dos en
pantalla.

**No hay base de datos.** Los Excel son el archivo. Lo único que la aplicación recuerda entre
consultas es la lectura anterior, en memoria, para poder calcular el movimiento intradía; al cerrar
la aplicación se pierde y no pasa nada.

## Instalación

Necesitas **Java 25 o superior** y un escritorio con ventanas (no funciona en un servidor sin
pantalla).

1. Descarga el `.zip` de la [última versión](../../releases/latest) y descomprímelo.
2. Abre `bvl.properties` y ajusta al menos `xlsPath`, que es dónde quieres los Excel.
3. Ejecuta `ejecutar.bat` (Windows) o `java -jar bvl-<versión>.jar --spring.config.location=bvl.properties`.

## Configuración

Todo se ajusta en `bvl.properties`, junto al jar. El horario y el umbral también se pueden cambiar
sobre la marcha desde la ventana: escribe el valor y pulsa Enter.

| Propiedad | Qué hace |
|---|---|
| `xlsPath` | carpeta raíz donde se crean los Excel |
| `alarma` | umbral de variación en %, a partir del cual avisa |
| `horaInicio`, `horaFin` | franja en la que consulta, formato `H:mm:ss` |
| `intervalo` | cada cuánto consulta; minutos enteros divisores de 60 (5, 10, 15, 20, 30…) |
| `alertaTimeout` | cuánto aguanta abierto un aviso antes de cerrarse solo; por defecto 10 minutos |
| `baseUrl`, `urlCotizaciones`, `urlHora` | direcciones de la API de la BVL |

Un aviso que ahorra un rato: `--spring.config.location` **sustituye** a la configuración interna en
vez de completarla, así que `bvl.properties` tiene que llevar todas las propiedades de la tabla
menos `alertaTimeout`, que tiene valor por defecto.

## Arquitectura

Java 25 y Spring Boot 4.1, con la interfaz en Swing. Una consulta recorre estas piezas:

```
BvlScheduler   cuándo se consulta: cron de lunes a viernes + franja horaria
  └─ CicloSondeo        el recorrido completo de una lectura
       ├─ LectorBvl       habla con la BVL y traduce el JSON al dominio en castellano
       └─ ExportService   vuelca la lectura a los dos Excel
  └─ VentanaPrincipal   presenta el resultado, siempre en el hilo de la interfaz
       └─ DetectorVariaciones → AlertaFormatter → bandeja del sistema + aviso
```

Y así está repartido el código:

| Paquete | De qué se ocupa |
|---|---|
| `market` | pedir los datos a la BVL y traducirlos |
| `export` | escribir los Excel (Apache POI) |
| `schedule` | cuándo se consulta y el recorrido de cada lectura |
| `alert` | qué acciones merecen aviso y cómo se redacta |
| `ui` | la ventana, la bandeja y los avisos |
| `config` | configuración y arranque |
| `domain` | `Accion`, `Item`, `Moneda`, `Sector` |

El dominio y los comentarios están **en castellano** a propósito. Si vas a tocar el código, en
[`CLAUDE.md`](CLAUDE.md) está el detalle: decisiones de diseño, trampas conocidas y por qué algunas
cosas son como son.

## Compilar

Usa el wrapper de Maven; no hace falta tener Maven instalado.

```bash
./mvnw clean package    # genera el jar en target/
./mvnw test             # 116 tests; no tocan la red ni abren ventanas
./mvnw spring-boot:run  # abre la ventana
```

En macOS y Linux acuérdate de cambiar `xlsPath`: el valor por defecto es una ruta de Windows.

## Contribuir

Las incidencias y las pull requests son bienvenidas. Antes de empezar, lee
[CONTRIBUTING.md](CONTRIBUTING.md); al participar aceptas el
[código de conducta](CODE_OF_CONDUCT.md).

## Licencia

[GNU Lesser General Public License v3.0](LICENSE) o posterior. El texto completo está en
[`LICENSE`](LICENSE) (LGPL) y [`COPYING`](COPYING) (GPL, a la que la LGPL se remite).

Puedes usar, estudiar, modificar y redistribuir el programa. Si distribuyes una versión modificada,
tienes que publicar esos cambios con la misma licencia.

Copyright © Jorge Neyra

Este programa se distribuye con la esperanza de que sea útil, pero **SIN NINGUNA GARANTÍA**, ni
siquiera la garantía implícita de comerciabilidad o idoneidad para un propósito particular.
