# Notas de versión

Aquí vivían las notas que antes estaban en el `README.md`.

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
