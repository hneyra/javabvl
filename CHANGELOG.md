# Notas de versión

Aquí vivían las notas que antes estaban en el `README.md`.

## Próxima versión

### Correcciones

* **El sondeo cuenta desde la hora de inicio.** Con 9:45 cada 20 minutos se consultaba a las 10:00,
  10:20, 10:40…, porque el planificador disparaba sobre la rejilla del reloj. Ahora consulta a las
  9:45, 10:05, 10:25…
* Las horas de la ventana se interpretan siempre en **hora de Lima**. Antes valía la zona del
  equipo, así que en una máquina fuera de Perú el horario quedaba desplazado respecto al mercado.
* El sondeo de la hora de fin ya no se pierde. El disparo siempre llegaba unos milisegundos tarde y
  quedaba fuera de la ventana.
* **Un horario mal escrito ya no se descarta en silencio.** Al pulsar Iniciar con un horario no
  válido, los campos volvían a 9:40 y el sondeo arrancaba con el horario anterior; el motivo quedaba
  tapado en la barra de estado. Ahora un aviso explica qué falla, lo tecleado se conserva para
  corregirlo e Iniciar no arranca.

### Mejoras

* El intervalo ya no tiene que dividir a 60: vale cualquier número de minutos que quepa en la franja.

## 5.0.0

### Cambios incompatibles

* **Se elimina la base de datos embebida (H2).** Las cotizaciones van de la BVL directamente al XLS.
  Desaparecen los endpoints REST `/item`, `/accion`, `/moneda` y `/lectura`. Los ficheros `bvldb.*`
  de instalaciones anteriores quedan huérfanos: no se borran, simplemente dejan de usarse, y las
  propiedades `spring.datasource.*` y `spring.jpa.*` ya no hacen nada.

### Nuevas funcionalidades

* Alerta también por **movimiento desde el sondeo anterior**, no solo contra el cierre del día
  previo. Se compara en memoria; el primer sondeo tras arrancar no tiene con qué comparar.

### Correcciones

* El XLS del día ya no acumula filas repetidas cuando la BVL publica dos veces el mismo instante.

### Mejoras

* Reestructuración completa del código por capas (`market`, `export`, `alert`, `schedule`,
  `service`, `ui`, `config`), inyección por constructor y configuración centralizada.
* El jar pasa de 83,5 MB a 52,4 MB.
* De 89 a 116 tests.

## 4.0.0

### Correcciones

Ninguna

### Mejoras

* Lectura desde la nueva URL y el nuevo formato JSON

### Nuevas funcionalidades

Ninguna

## 3.0.1

### Correcciones

* La variación de las acciones no se generaba correctamente en el mensaje.

### Mejoras

Ninguna

### Nuevas funcionalidades

Ninguna
