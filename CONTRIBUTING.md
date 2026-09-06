# Cómo contribuir

Gracias por echar una mano. Este documento es corto a propósito: solo lo que hace falta saber para
que tu cambio entre sin fricción.

Al participar aceptas el [código de conducta](CODE_OF_CONDUCT.md).

## Antes de escribir código

- **Para un fallo**, abre una incidencia con la versión, el sistema operativo y qué esperabas que
  pasara. Si la aplicación mostró un error, pega el texto.
- **Para una funcionalidad nueva**, abre una incidencia antes de programar nada. Es más rápido
  descartar una idea en tres frases que en una pull request de trescientas líneas.
- **Para un arreglo pequeño y evidente**, ve directo a la pull request.

## Poner en marcha el proyecto

Necesitas **JDK 25**. Maven no: usa el wrapper.

```bash
./mvnw test                    # toda la suite
./mvnw test -Dtest=NombreTest  # una clase suelta
./mvnw clean package           # el jar
./mvnw spring-boot:run         # abre la ventana
```

En macOS y Linux cambia `xlsPath` en `src/main/resources/application.properties`: el valor por
defecto es una ruta de Windows.

## Reglas de la casa

Son pocas, pero saltárselas rompe cosas de formas que no se ven hasta producción.

**El dominio va en castellano.** `Accion`, `Lectura`, `Moneda`, `Sector`, y los nombres nuevos
igual. Los nombres de paquete, en inglés. Los DTO de `market/dto` conservan los nombres en inglés
del JSON de la BVL, erratas incluidas: son el contrato con su API, no nuestro modelo.

**Inyección por constructor**, nunca sobre campos. **Logging con SLF4J y placeholders** (`{}`), no
concatenando cadenas.

**Los tests se llaman `*Test` o `*IntegrationTest`, nunca `*IT`.** Surefire no recoge ese patrón: un
test con ese nombre queda fuera de `./mvnw test` sin avisar de nada.

**No toques las trampas conocidas sin preguntar.** Hay comportamientos que parecen fallos y lo son,
pero están sujetos por tests de caracterización porque alguien puede depender de ellos. Llevan un
Javadoc `TRAMPA CONOCIDA:` y están listados en [`CLAUDE.md`](CLAUDE.md). Si arreglas uno, actualiza
su test explicando el cambio; no lo borres.

**Si añades una propiedad de configuración sin valor por defecto**, añádela también a
`deploy/bvl.properties` y a la lista de `DeployPropertiesTest`. El arranque en producción usa
`--spring.config.location`, que **sustituye** al fichero empaquetado en vez de completarlo: una
propiedad que falte allí solo revienta en la máquina del usuario final. Ese test existe para
cazarlo en el build.

**Si cambias el formato del Excel**, ten en cuenta que hay años de ficheros ya generados con las
rutas, los nombres de hoja y el orden de columnas actuales. Cambiarlos parte el histórico en dos.

## Tests

Cualquier cambio de comportamiento viene con su test. La suite no toca la red, ni el disco del
usuario, ni abre ventanas, y así debe seguir: usa `@TempDir` para ficheros y un `WebClient` de
mentira para la API.

Antes de abrir la pull request, comprueba que pasa todo:

```bash
./mvnw clean test
```

## Pull requests

- Una pull request, un tema.
- Explica **por qué**, no solo qué. El qué se lee en el diff.
- Si cambia algo que el usuario nota, dilo claramente y añádelo a [`CHANGELOG.md`](CHANGELOG.md).
- **No subas la versión del POM.** La lleva el workflow de release automáticamente en cada push a
  `main`. Solo se edita a mano para saltar de versión menor o mayor.

Los mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/es/):
`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`. Un `!` detrás del tipo marca un cambio
incompatible.

## Licencia

Al enviar una contribución aceptas que se publique bajo la
[LGPL v3](LICENSE), la misma licencia del proyecto.
