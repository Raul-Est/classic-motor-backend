# Módulo 03 · Diseño de APIs REST

**Desarrollo Web en Entorno Servidor · 2º DAW**

## Antes de empezar

**Para qué sirve este módulo.** En el módulo 02 consumiste APIs que había diseñado otra persona. Aquí aprendes a **diseñar la tuya**: qué recursos expone, cómo se llaman sus URIs, qué método usa cada operación, qué código de estado devuelve, cómo describe los errores, cómo se filtra y pagina, cómo evoluciona sin romper a quien la usa, cómo se protege y cómo se escribe todo eso en un **contrato** (OpenAPI) antes de programar una sola línea. Es justo lo que tendrás que hacer al empezar el proyecto P1: decidir el diseño del API de tu dominio. En la defensa te preguntarán por qué una operación usa PATCH y no PUT, o por qué devuelve 409 y no 400, así que no basta con que funcione: tienes que saber justificarlo.

**Hilo conductor.** Seguimos con la **biblioteca**. Al terminar el módulo tendrás el diseño completo de su API: modelo de recursos (03.01), tabla de endpoints (03.02), códigos y errores (03.03), consultas y paginación (03.04), política de versiones (03.05), matriz de permisos (03.06) y el contrato `library-api.yaml` (03.07). Es lo que programaremos con Spring Boot a partir del módulo 06. Para que practiques el traslado a otro dominio, como harás en el P1, las prácticas usan un **gimnasio** (clases, monitores, socios y reservas). Para ver cada idea en acción seguimos usando APIs públicas reales (Rick and Morty, DummyJSON, Frankfurter, PokeAPI, GitHub y Postman Echo) y, cuando hace falta un API que guarde los cambios, una que levantas tú en tu ordenador con json-server.

**Qué necesitas.**

| Herramienta | Versión | Dónde | Para qué |
|---|---|---|---|
| curl | La de tu sistema | Ya instalado (módulo 02) | Todas las secciones |
| Bruno | 4.x (comprobado con 4.3.0) | Ya instalado (módulo 02) | 03.02 y 03.06 |
| Node.js | 26 LTS (comprobado con 26.13.0) | https://nodejs.org | 03.02 (json-server) y 03.07 (Redocly CLI y Prism), siempre con `npx` |
| Navegador | Chrome, Edge o Firefox | Ya instalado | 03.07: Swagger Editor en https://editor.swagger.io |
| Editor de texto | VS Code (recomendado) | Ya instalado | Ficheros `.txt`, `.json` y `.yaml` |

No vamos a programar en JavaScript. Node.js hace falta porque tres herramientas que usamos se distribuyen así: **json-server**, que convierte un fichero JSON en un API REST en tu ordenador (03.02), **Redocly CLI**, que valida contratos OpenAPI, y **Prism**, que simula un API a partir de su contrato (03.07). Con `npx` se descargan y ejecutan sin instalarlas. Si ya tienes Node por otro módulo y su versión es 24 o superior, te vale.

**Instalación y comprobación.**

1. Descarga el instalador **LTS** de Node.js desde su web e instálalo con las opciones por defecto. Cierra y vuelve a abrir el terminal.
2. Comprueba la versión de Node y de npx:

   ```bash
   node --version
   npx --version
   ```

   ✅ **Si ves esto, vas bien:** `v26.` algo en la primera línea y `11.` algo en la segunda (la versión de npm que trae Node 26).

3. Comprueba que curl y Bruno siguen funcionando como en el módulo 02 (`curl.exe --version` en PowerShell y la pantalla de inicio de Bruno).

**Cómo usar el código de este módulo.** El ZIP tiene una carpeta por sección, como en el módulo 02:

- Los ficheros `.sh` contienen **un único comando**. Cópialo y pégalo en el terminal (en PowerShell, cambia `curl` por `curl.exe`), o ejecútalo con `bash fichero.sh` en macOS, Linux o Git Bash.
- Los `.txt` son documentos de diseño: tablas de recursos, endpoints, códigos o permisos. En este módulo son tan importantes como el código: son lo que tendrás que escribir para tu proyecto.
- Los `.json` son ejemplos de representaciones y de errores. Las carpetas con `opencollection.yml` son colecciones de Bruno.
- `library-api.yaml` es el contrato OpenAPI (03.07).

> Las salidas son reales, capturadas el 2 de noviembre de 2026 con curl 7.81, Bruno CLI 4.3.0, Node.js 26.13.0, Redocly CLI 2.72.1 y Prism 5.16.0. Las fechas, los identificadores y algunos valores cambiarán cuando lo pruebes tú. De las cabeceras se muestran solo las que se comentan en el texto.

| Error frecuente | Causa | Solución |
|---|---|---|
| `npx : File C:\Program Files\nodejs\npx.ps1 cannot be loaded because running scripts is disabled on this system.` | PowerShell no permite ejecutar scripts y `npx` es uno | Escribe `npx.cmd` en lugar de `npx`, o permite scripts para tu usuario: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| `node : The term 'node' is not recognized as the name of a cmdlet, function, script file, or operable program.` | No has reabierto el terminal después de instalar, o Node no está en el `PATH` | Cierra y abre el terminal; si sigue, reinstala marcando "Add to PATH" |
| `Need to install the following packages: @redocly/cli@2.72.1 Ok to proceed? (y)` | No es un error: la primera vez, `npx` descarga la herramienta | Responde `y` y pulsa Intro |

---

## 03.01 · Principios REST y recursos

### Resumen
- **REST** (*Representational State Transfer*) es un estilo de arquitectura, no un protocolo ni una librería. Lo definió Roy Fielding en el año 2000 a partir de cómo funciona la web.
- Un API REST se organiza en **recursos** (cosas con nombre: un libro, un socio, la lista de préstamos), cada uno identificado por una **URI**.
- El cliente nunca recibe el recurso, sino una **representación** suya (normalmente JSON). Un mismo recurso puede tener varias representaciones y el cliente pide la que quiere con `Accept`.
- Las restricciones que más afectan a tu diseño: **cliente-servidor**, **sin estado** (cada petición lleva todo lo necesario), **cacheable** e **interfaz uniforme** (mismos métodos y códigos para todos los recursos).
- Diseñar un API empieza por el **modelo de recursos**: colecciones, elementos, subrecursos, identificadores y relaciones.

### Teoría

**REST no es "JSON por HTTP".** Muchas APIs que se anuncian como REST solo usan HTTP como transporte: una única URL (`/api`) a la que se envía todo por POST con un campo `"accion": "borrarLibro"`. Funciona, pero desaprovecha lo que HTTP ya ofrece: métodos con significado, códigos de estado, cachés, cabeceras. REST propone usar la web **como fue diseñada**, y por eso cualquier cliente HTTP (curl, Bruno, el navegador, una app móvil) sabe trabajar con un API REST sin conocerla de antemano.

**Las restricciones de REST.** Fielding definió REST como un conjunto de restricciones. Si un API las cumple, gana escalabilidad, sencillez y evolución independiente de cliente y servidor.

| Restricción | Qué exige | Qué significa para tu API |
|---|---|---|
| Cliente-servidor | Cliente y servidor separados, comunicándose por un interfaz | El servidor no sabe si el cliente es una web, una app o Bruno (02.01) |
| Sin estado (*stateless*) | Cada petición lleva todo lo necesario para entenderla | Nada de "la página actual" o "el usuario logueado" guardados en el servidor entre peticiones: el token va en cada petición (03.06) |
| Cacheable | Las respuestas dicen si se pueden guardar en caché | `Cache-Control`, `ETag` (02.03) |
| Interfaz uniforme | Recursos identificados por URI, manipulados mediante representaciones, mensajes autodescriptivos y enlaces | Mismos métodos y códigos para todos los recursos (03.02 y 03.03) |
| Sistema en capas | El cliente no sabe si habla con el servidor final o con un intermediario | Proxies, balanceadores, el gateway de los microservicios (14.04) |
| Código bajo demanda (opcional) | El servidor puede enviar código ejecutable | Es lo que hace una web con JavaScript; en un API no se usa |

**Sin estado, con un ejemplo.** Imagina un API que, tras `GET /books?page=1`, recuerda en qué página está cada cliente para que `GET /books/next` devuelva la siguiente. Ese servidor tiene que guardar información de cada cliente, no puede repartirse en varias copias (la siguiente petición podría llegar a otra que no sabe nada) y falla si se reinicia. La versión REST es que el cliente pida `GET /books?page=2`: la petición se entiende sola. Lo mismo con la identidad: el servidor no "recuerda" que has hecho login; el cliente envía el token **en cada petición**.

**Recurso, URI y representación.**
- **Recurso**: cualquier cosa con nombre que merezca ser consultada o modificada: el libro con ISBN 9788437604947, el socio 7, la colección de todos los libros, los préstamos del socio 7.
- **URI**: su identificador. `/api/v1/books/9788437604947`. Un recurso tiene una URI y una URI identifica un único recurso.
- **Representación**: lo que viaja. El JSON de un libro es una representación del libro, no el libro (en la base de datos puede estar repartido en tres tablas). El mismo recurso puede representarse en JSON, XML, CSV o HTML, y con más o menos campos (una lista lleva un resumen; el detalle, todo).

**Negociación de contenido.** El cliente dice qué representación quiere con `Accept`. Si el servidor no puede dársela, HTTP (RFC 9110) le deja dos opciones: responder **406 Not Acceptable** o ignorar `Accept` y enviar su representación por defecto. Spring Boot elige la primera: si pides XML a un API que solo produce JSON, obtienes 406. En el ejemplo verás que cada API pública hace una cosa distinta.

**Tipos de recursos.**

| Tipo | Ejemplo | Qué es |
|---|---|---|
| Colección | `/api/v1/books` | El conjunto de todos los libros. Se lee (GET) y se le añaden elementos (POST) |
| Elemento | `/api/v1/books/9788437604947` | Un libro concreto. Se lee, se reemplaza, se modifica y se borra |
| Subrecurso | `/api/v1/members/7/loans` | Recursos que solo tienen sentido dentro de otro: los préstamos **del socio 7** |
| Singleton | `/api/v1/members/7/address` | Un único recurso dentro de otro: la dirección del socio 7 |

**Identificadores.** Cada elemento necesita un identificador estable. Hay dos opciones:
- **Natural**: un dato que ya existe y es único, como el ISBN de un libro. Es legible y no cambia, pero no todos los dominios lo tienen.
- **Sustituto**: un número o un UUID que asigna el servidor (`/members/7`, `/objects/ff808181...`). Siempre está disponible. Si es secuencial, deja adivinar cuántos hay y probar los siguientes (lo verás en 03.06).

Nunca uses como identificador algo que pueda cambiar (el nombre, el email).

**Hipermedia y modelo de madurez de Richardson.** Leonard Richardson clasificó las APIs en cuatro niveles según cuánto aprovechan la web:

| Nivel | Qué usa | Ejemplo |
|---|---|---|
| 0 | HTTP como transporte: una URL y todo por POST | `POST /api` con `{"accion": "buscarLibro"}` |
| 1 | Recursos: una URI por recurso | `POST /api/books/9788437604947` para todo |
| 2 | Recursos + métodos y códigos HTTP con su significado | `GET`, `POST`, `DELETE`… y 200, 201, 404… |
| 3 | Lo anterior + **enlaces** en las respuestas (HATEOAS) | La respuesta de un libro trae la URL de su autor |

La mayoría de las APIs profesionales están en el **nivel 2**, que es el objetivo de este curso. El nivel 3 (*Hypermedia As The Engine Of Application State*) añade enlaces para que el cliente navegue sin construir URLs. Rick and Morty lo hace de forma sencilla: cada personaje lleva la URL de su lugar y de sus episodios. PokeAPI y la paginación de 03.04 también usan enlaces (`next`, `previous`).

**Cómo se diseña un modelo de recursos.**
1. Lee los requisitos y subraya los **sustantivos**: libro, autor, socio, préstamo. Son candidatos a recurso.
2. Decide cuáles son recursos con identidad propia (se consultan solos) y cuáles son solo datos de otro (la dirección del socio).
3. Elige el identificador de cada uno.
4. Dibuja las **relaciones** (un socio tiene muchos préstamos) y decide qué relaciones se exponen como subrecurso.
5. Decide la **representación**: qué campos lleva en la lista y en el detalle (02.04). Recuerda que la representación no tiene por qué coincidir con lo que se guarda.

Las acciones (prestar, devolver, cancelar) también se modelan como recursos o cambios de estado de un recurso: prestar un libro es **crear un préstamo**. Lo concretamos en 03.02.

### Código de ejemplo

**1. El punto de entrada de un API con enlaces.** La raíz de Rick and Morty no devuelve datos, sino las URLs de sus tres colecciones.

`01-raiz.sh`
```bash
curl https://rickandmortyapi.com/api/
```

✅ **Si ves esto, vas bien:**
```text
{"characters":"https://rickandmortyapi.com/api/character","locations":"https://rickandmortyapi.com/api/location","episodes":"https://rickandmortyapi.com/api/episode"}
```

**2. Un elemento y sus relaciones.** El personaje 147 trae su identificador, su propia URL y **enlaces** a otros recursos: su lugar (`location.url`) y los episodios en los que aparece (`episode`). El cliente no tiene que saber cómo se construyen esas URLs; solo seguirlas.

`02-personaje.sh`
```bash
curl https://rickandmortyapi.com/api/character/147
```

```text
{"id":147,"name":"Gobo","status":"Dead","species":"Alien","type":"","gender":"Male","origin":{"name":"unknown","url":""},"location":{"name":"Earth (Replacement Dimension)","url":"https://rickandmortyapi.com/api/location/20"},"image":"https://rickandmortyapi.com/api/character/avatar/147.jpeg","episode":["https://rickandmortyapi.com/api/episode/29"],"url":"https://rickandmortyapi.com/api/character/147","created":"2017-12-29T11:38:29.578Z"}
```

Fíjate también en `origin`: cuando no se conoce, la URL va vacía (`""`). Un diseño más limpio usaría `null` (02.04).

**3 y 4. Negociación de contenido.** Pedimos XML a dos APIs que solo producen JSON. A PokeAPI le basta con ver el código y el tipo de contenido (`-o /dev/null` descarta el cuerpo):

`03-accept-pokeapi.sh`
```bash
curl -s -o /dev/null -w "HTTP %{http_code} | %{content_type}\n" -H "Accept: application/xml" https://pokeapi.co/api/v2/pokemon/25
```

```text
HTTP 200 | application/json; charset=utf-8
```

`04-accept-github.sh`
```bash
curl -i -H "Accept: application/xml" https://api.github.com/versions
```

```text
HTTP/2 415 
date: Mon, 02 Nov 2026 09:23:07 GMT
content-type: application/json; charset=utf-8
x-ratelimit-limit: 60
x-ratelimit-remaining: 57

{
  "message": "Unsupported 'Accept' header: 'application/xml'. Must accept 'application/json'.",
  "documentation_url": "https://docs.github.com/v3/media",
  "status": "415"
}
```

PokeAPI **ignora** `Accept` y devuelve JSON con 200: está permitido, y el cliente debe mirar `content-type` para saber qué ha recibido. GitHub rechaza la petición y explica por qué, pero con **415 Unsupported Media Type**, que es el código para un **cuerpo** que el servidor no entiende. Para un `Accept` que no puede atender, el código correcto es **406**, que es lo que devolverá tu API de Spring Boot. Incluso las APIs más usadas tienen fallos de diseño (en 03.03 verás más).

**5. Otra representación del mismo recurso.** DummyJSON permite pedir solo algunos campos con `select`. El recurso es el mismo producto; la representación, más pequeña.

`05-representacion-parcial.sh`
```bash
curl "https://dummyjson.com/products/1?select=title,price"
```

```text
{"id":1,"title":"Essence Mascara Lash Princess","price":9.99}
```

**6. El modelo de recursos de la biblioteca.** Este es el resultado de aplicar los cinco pasos de la teoría. Es el primer documento de diseño del API que programaremos.

`recursos-biblioteca.txt`
```text
MODELO DE RECURSOS · API de la biblioteca (v1)

Colecciones (sustantivo en plural)      Elemento (un recurso concreto)
-------------------------------------   --------------------------------------
/api/v1/books                           /api/v1/books/{isbn}
/api/v1/authors                         /api/v1/authors/{id}
/api/v1/members                         /api/v1/members/{id}
/api/v1/loans                           /api/v1/loans/{id}

Subrecursos (dependen de otro recurso)
-------------------------------------
/api/v1/authors/{id}/books              libros de un autor
/api/v1/members/{id}/loans              préstamos de un socio

Identificadores
- Libro: ISBN (identificador natural, ya existe y es único).
- Autor, socio y préstamo: id numérico que asigna el servidor.

Relaciones
- Un autor escribe muchos libros; un libro tiene un autor (simplificado).
- Un socio tiene muchos préstamos; un préstamo es de un socio y de un libro.

Representaciones (lo que viaja, no lo que se guarda)
- Book en la lista:   {isbn, title, available}
- Book en el detalle: {isbn, title, author{id, name}, publishedYear, pages,
                       available, copies, genres}
- Loan:               {id, book{isbn, title}, member{id, name}, loanDate,
                       dueDate, returnDate, status}
```

Decisiones que conviene saber justificar:
- El libro se identifica por **ISBN** porque ya es único y estable. Autor, socio y préstamo no tienen identificador natural: el servidor les asigna un número.
- El préstamo es un **recurso propio** (`/loans/{id}`), no solo un subrecurso del socio, porque se consulta y se modifica por sí mismo (al devolverlo). Además se accede a los préstamos de un socio con `/members/{id}/loans`: dos URIs que llevan a los mismos datos desde distintos puntos de vista.
- Las representaciones de la lista y del detalle son distintas: la lista lleva lo justo para mostrar un listado.

| Error frecuente | Causa | Solución |
|---|---|---|
| Un único endpoint `POST /api` con un campo `"accion"` | Pensar en funciones (RPC) en lugar de recursos | Identifica los sustantivos y dales una URI a cada uno |
| Usar el email o el nombre como identificador en la URI | Elegir un dato que puede cambiar | Usa un identificador estable: natural (ISBN) o asignado por el servidor |
| El servidor guarda "en qué página va cada usuario" | Romper la restricción sin estado | Que el cliente envíe en cada petición lo que necesita (`?page=2`) |
| Respuesta `406` (o `415` en GitHub) al pedir un recurso que existe | Has pedido con `Accept` un formato que el API no produce | Pide `application/json` o quita la cabecera `Accept` |
| `https://rickandmortyapi.com/api` devuelve una página HTML de `301 Moved Permanently` | Falta la barra final; el servidor redirige a `/api/` | Escribe la URL con la barra final o añade `-L` |

### Práctica

1. **Seguir enlaces.** Pide con curl el personaje 147 de Rick and Morty (como en el ejemplo) y, **sin construir tú ninguna URL**, sigue los enlaces de su respuesta:
   1. Descarga su lugar en `lugar.json`, mostrando solo el código, el tipo de contenido y el tamaño.
   2. Haz lo mismo con su episodio en `episodio.json`.
   3. Abre los ficheros. ¿Cuántos residentes tiene el lugar? ¿Cómo se llama el episodio, y cuántos personajes aparecen en él?
2. **Negociación de contenido.** Pide la lista de monedas de Frankfurter (`https://api.frankfurter.dev/v2/currencies`) y la lista de versiones de GitHub (`https://api.github.com/versions`) aceptando solo `text/csv`. Muestra únicamente el código y el tipo de contenido. ¿Qué hace cada una? ¿Cuál se ajusta a HTTP?
3. **Modelo de recursos del gimnasio.** Escribe `recursos-gimnasio.txt`, con el formato de `recursos-biblioteca.txt`, para estos requisitos:

   > El gimnasio organiza **clases** (spinning, yoga, pilates…) con fecha y hora, sala y un número máximo de plazas. Cada clase la imparte un **monitor**. Los **socios** se dan de alta con nombre, email y teléfono, y **reservan** plaza en las clases. Una reserva se puede cancelar. Los socios quieren ver sus reservas; los monitores, quién ha reservado en sus clases.

   Incluye colecciones, elementos, subrecursos, identificadores, relaciones y representaciones.
4. Escribe en `clase.json` la representación de **detalle** de una clase, coherente con tu modelo (02.04: fechas ISO, objetos anidados, `null`).
5. Responde:
   1. ¿Por qué la reserva es un recurso propio y no un campo de la clase?
   2. Un compañero propone `GET /api/v1/classes/next` para "la siguiente clase que el socio todavía no ha visto en la app". ¿Qué restricción de REST incumple y cómo lo arreglarías?
   3. ¿En qué nivel de Richardson está Rick and Morty? ¿Qué le falta (o le sobra) para estar en el siguiente?

> **Pista:** en el apartado 1, `-s -o lugar.json -w "HTTP %{http_code} | %{content_type} | %{size_download} bytes\n"`. Para contar elementos de una lista larga, abre el JSON en VS Code y pliega el array, o fíjate en el número de línea donde termina.

### Solución

**1. Seguir enlaces.** El personaje 147 trae `location.url` = `https://rickandmortyapi.com/api/location/20` y un único episodio, `https://rickandmortyapi.com/api/episode/29`. Se copian de la respuesta tal cual.

`01-lugar.sh`
```bash
curl -s -o lugar.json -w "HTTP %{http_code} | %{content_type} | %{size_download} bytes\n" https://rickandmortyapi.com/api/location/20
```

```text
HTTP 200 | application/json; charset=utf-8 | 11223 bytes
```

`02-episodio.sh`
```bash
curl -s -o episodio.json -w "HTTP %{http_code} | %{content_type} | %{size_download} bytes\n" https://rickandmortyapi.com/api/episode/29
```

```text
HTTP 200 | application/json; charset=utf-8 | 1141 bytes
```

Lo que contienen los ficheros:

```text
residents: 230 | name: Earth (Replacement Dimension) | type: Planet | dimension: Replacement Dimension
characters: 20 | name: Morty's Mind Blowers | episode: S03E08 | air_date: September 17, 2017
```

El lugar ocupa 11 KB porque lleva 230 enlaces, uno por residente. Es el precio de los enlaces: el cliente puede navegar, pero las respuestas crecen. Por eso las colecciones grandes se paginan (03.04).

**2. Negociación de contenido.**

`03-accept-csv.sh`
```bash
curl -s -o /dev/null -w "HTTP %{http_code} | %{content_type}\n" -H "Accept: text/csv" https://api.frankfurter.dev/v2/currencies
```

```text
HTTP 200 | application/json; charset=utf-8
```

`04-accept-csv-github.sh`
```bash
curl -s -o /dev/null -w "HTTP %{http_code} | %{content_type}\n" -H "Accept: text/csv" https://api.github.com/versions
```

```text
HTTP 415 | application/json; charset=utf-8
```

Frankfurter ignora `Accept` y envía JSON con 200, algo que HTTP permite. GitHub rechaza la petición con 415, pero el código adecuado sería 406: 415 se refiere al formato del cuerpo que envía el cliente, y esta petición no tiene cuerpo.

**3. Modelo de recursos.**

`recursos-gimnasio.txt`
```text
MODELO DE RECURSOS · API del gimnasio (v1) · solución

Colecciones                            Elementos
-------------------------------------  --------------------------------------
/api/v1/classes                        /api/v1/classes/{id}
/api/v1/trainers                       /api/v1/trainers/{id}
/api/v1/members                        /api/v1/members/{id}
/api/v1/bookings                       /api/v1/bookings/{id}

Subrecursos
-------------------------------------
/api/v1/members/{id}/bookings          reservas de un socio
/api/v1/classes/{id}/bookings          quién ha reservado en una clase
/api/v1/trainers/{id}/classes          clases que imparte un monitor

Identificadores
- Todos numéricos, asignados por el servidor: no hay identificador natural
  estable (el email de un socio puede cambiar).

Relaciones
- Un monitor imparte muchas clases; una clase tiene un monitor.
- Un socio hace muchas reservas; una reserva es de un socio y de una clase.
- Una clase tiene muchas reservas (como mucho, tantas como plazas).

Representaciones
- Class en la lista:   {id, type, startTime, freeSpots}
- Class en el detalle: {id, type, startTime, durationMinutes, room,
                        capacity, freeSpots, trainer{id, name}}
- Booking:             {id, class{id, type, startTime}, member{id, name},
                        createdAt, status}   status: CONFIRMED | CANCELLED
- Member:              {id, name, email, phone}   (nunca datos de pago)

Notas
- freeSpots no se guarda: se calcula (capacity - reservas confirmadas).
  La representación no tiene por qué coincidir con lo que se guarda.
- La sala (room) es un dato de la clase, no un recurso: nadie la consulta
  por separado.
```

**4. Representación de una clase.**

`clase.json`
```json
{
  "id": 31,
  "type": "spinning",
  "startTime": "2026-11-10T18:00:00",
  "durationMinutes": 45,
  "room": "Sala 2",
  "capacity": 20,
  "freeSpots": 4,
  "trainer": {
    "id": 4,
    "name": "Marta Gil"
  }
}
```

**5. Preguntas.**
1. Porque tiene identidad y ciclo de vida propios: se crea, se consulta, se cancela, tiene un estado y una fecha. Si fuera un campo de la clase (una lista de socios), cancelar una reserva obligaría a modificar la clase entera y no habría forma de pedir "las reservas del socio 7" sin recorrer todas las clases.
2. Incumple **sin estado**: el servidor tendría que recordar qué ha visto cada socio en la app. Lo correcto es que la petición lleve todo lo necesario: `GET /api/v1/classes?from=2026-11-02T18:00:00&sort=startTime,asc&size=1` (filtros y orden, 03.04).
3. En el **nivel 2**, con algo del 3: usa recursos, GET y códigos (404 cuando no existe) y además incluye enlaces a recursos relacionados. Para un nivel 3 completo, las respuestas también dirían qué **acciones** se pueden hacer (por ejemplo, un enlace para reservar) y no solo a qué otros recursos se puede ir. Al ser de solo lectura, Rick and Morty no tiene acciones que enlazar.

---

## 03.02 · URIs, verbos HTTP e idempotencia

### Resumen
- La **URI** dice sobre qué recurso se actúa y el **método** dice qué se hace. Las URIs llevan **sustantivos en plural**, en minúsculas y en *kebab-case*; nunca verbos.
- Patrón base: `GET /books`, `POST /books`, `GET /books/{id}`, `PUT /books/{id}`, `PATCH /books/{id}`, `DELETE /books/{id}`.
- Las relaciones se expresan con **subrecursos** (`/members/7/loans`) y los filtros con **parámetros de consulta** (`?status=ACTIVE`).
- **Seguro**: no cambia nada (GET, HEAD, OPTIONS). **Idempotente**: repetirlo deja el servidor igual que hacerlo una vez (los seguros, PUT y DELETE). POST **no** es idempotente; PATCH, no necesariamente.
- La idempotencia importa porque las redes fallan: un cliente puede repetir sin miedo una petición idempotente que no sabe si llegó.

### Teoría

**Reglas para las URIs.** Las URIs son el interfaz que más se ve de tu API y, una vez publicadas, cambiarlas rompe a los clientes. Las convenciones del curso (ficha del curso) son:

| Regla | Bien | Mal |
|---|---|---|
| Prefijo y versión | `/api/v1/books` | `/books`, `/rest/libros` |
| Sustantivos en plural | `/api/v1/members` | `/api/v1/member`, `/api/v1/getMembers` |
| Minúsculas y *kebab-case* | `/api/v1/book-copies` | `/api/v1/bookCopies`, `/api/v1/Book_Copies` |
| Sin verbos: el verbo es el método | `DELETE /api/v1/books/{isbn}` | `POST /api/v1/books/{isbn}/delete` |
| Identificador en la ruta | `/api/v1/books/9788437604947` | `/api/v1/books?isbn=9788437604947` para un elemento |
| Filtros, orden y página en la consulta | `/api/v1/books?available=true` | `/api/v1/books/available` |
| Sin extensión de formato | `/api/v1/books` + `Accept` | `/api/v1/books.json` |
| Sin barra final | `/api/v1/books` | `/api/v1/books/` |
| Anidar como mucho un nivel | `/api/v1/members/7/loans` | `/api/v1/members/7/loans/1024/book` |

Los nombres van en **inglés**, como el código (ficha del curso). Sea cual sea el idioma, lo importante es ser **consistente** en todo el API.

**El patrón CRUD sobre una colección.**

| Método | URI | Acción | Éxito |
|---|---|---|---|
| GET | `/books` | Listar | 200 |
| POST | `/books` | Crear un elemento en la colección | 201 + `Location` |
| GET | `/books/{isbn}` | Leer un elemento | 200 |
| PUT | `/books/{isbn}` | Reemplazar el elemento entero | 200 (o 204) |
| PATCH | `/books/{isbn}` | Modificar algunos campos | 200 |
| DELETE | `/books/{isbn}` | Borrar | 204 |

Con esto se cubre la mayoría de los casos. Los códigos los razonamos en 03.03.

**Subrecurso o filtro.** Los libros de un autor se pueden pedir de dos formas: `/authors/12/books` (subrecurso) o `/books?authorId=12` (filtro). Las dos son correctas y muchas APIs ofrecen ambas, como verás con JSONPlaceholder. Criterio práctico:
- **Subrecurso** cuando la relación es fuerte y estable y el recurso hijo no tiene sentido sin el padre (los préstamos **de un socio**).
- **Filtro** cuando es un criterio más entre otros que se pueden combinar (`/books?authorId=12&available=true`).

**Acciones que no son CRUD.** Prestar, devolver, cancelar, aprobar… Hay tres formas de modelarlas, de más a menos REST:

| Opción | Ejemplo | Cuándo |
|---|---|---|
| Crear un recurso | Prestar = `POST /loans` | Si la acción produce algo con identidad propia (un préstamo) |
| Cambiar el estado | Devolver = `PATCH /loans/1024` con `{"status": "RETURNED"}` | Si la acción es un cambio de estado de un recurso que ya existe |
| Sub-recurso de acción | `POST /loans/1024/return` | Cuando ninguna de las anteriores encaja de forma natural (GitHub lo hace: `PUT /repos/{owner}/{repo}/pulls/{n}/merge`) |

En la biblioteca usamos las dos primeras: prestar crea un préstamo y devolver cambia su estado.

**Seguro e idempotente.** Lo viste en 02.03; ahora toca usarlo para diseñar. Según la especificación de HTTP (RFC 9110):
- Un método es **seguro** si el cliente no pide ningún cambio en el servidor. GET, HEAD y OPTIONS. Por eso **un GET nunca puede borrar ni modificar nada**: los navegadores precargan enlaces, los buscadores los siguen y las cachés los repiten.
- Un método es **idempotente** si enviar la misma petición N veces tiene el **mismo efecto** en el servidor que enviarla una. GET, HEAD, OPTIONS, PUT y DELETE.

| Método | Seguro | Idempotente | Por qué |
|---|---|---|---|
| GET | Sí | Sí | Solo lee |
| PUT | No | Sí | "Deja el recurso así": la segunda vez ya está así |
| DELETE | No | Sí | Tras el primero no existe; los siguientes no cambian nada (responden 404, pero el estado es el mismo) |
| POST | No | **No** | Cada POST crea un recurso nuevo |
| PATCH | No | No necesariamente | `{"status": "RETURNED"}` sí lo es; `{"copies": "+1"}` no |

Fíjate: idempotente se refiere al **estado del servidor**, no a la respuesta. El segundo DELETE responde 404 y el primero 200, pero el servidor queda igual.

**Por qué importa.** El cliente envía un POST para prestar un libro y la conexión se corta antes de recibir la respuesta. ¿Se creó el préstamo? No lo sabe. Si repite un PUT o un DELETE, no pasa nada. Si repite el POST, puede crear **dos** préstamos. Soluciones habituales:
- Diseñar con métodos idempotentes cuando el significado lo permite (PUT con un identificador que elige el cliente, como el ISBN).
- Reglas de negocio que impiden el duplicado: "un socio no puede tener dos préstamos activos del mismo libro" → el segundo POST recibe 409 (03.03).
- La cabecera **`Idempotency-Key`**: el cliente envía un identificador único con cada POST y el servidor, si ya lo ha procesado, devuelve la misma respuesta sin repetir la operación. La usan Stripe y otras APIs de pagos, y está en proceso de estandarización en el IETF.

### Código de ejemplo

**1. Subrecurso y filtro: dos URIs para los mismos datos.** JSONPlaceholder ofrece los comentarios de un post como subrecurso y como filtro. `_limit=1` es un parámetro propio de este API para devolver solo uno.

`01-subrecurso.sh`
```bash
curl "https://jsonplaceholder.typicode.com/posts/1/comments?_limit=1"
```

```text
[
  {
    "postId": 1,
    "id": 1,
    "name": "id labore ex et quam laborum",
    "email": "Eliseo@gardner.biz",
    "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"
  }
]
```

`02-filtro.sh`
```bash
curl "https://jsonplaceholder.typicode.com/comments?postId=1&_limit=1"
```

```text
[
  {
    "postId": 1,
    "id": 1,
    "name": "id labore ex et quam laborum",
    "email": "Eliseo@gardner.biz",
    "body": "laudantium enim quasi est quidem magnam voluptate ipsam eos\ntempora quo necessitatibus\ndolor quam autem quasi\nreiciendis et nam sapiente accusantium"
  }
]
```

Misma respuesta. La diferencia es de diseño: el subrecurso expresa la relación; el filtro permite combinarla con otros criterios.

**2. Los endpoints de la biblioteca.** Aplicando las reglas al modelo de recursos de 03.01:

`endpoints-biblioteca.txt`
```text
ENDPOINTS · API de la biblioteca (v1)            S = seguro · I = idempotente

Método  URI                                  Qué hace                          S  I
------  -----------------------------------  --------------------------------  -  -
GET     /api/v1/books                        Lista los libros                  S  I
GET     /api/v1/books/{isbn}                 Detalle de un libro               S  I
POST    /api/v1/books                        Da de alta un libro                    
PUT     /api/v1/books/{isbn}                 Reemplaza los datos de un libro      I
PATCH   /api/v1/books/{isbn}                 Cambia algunos campos               
DELETE  /api/v1/books/{isbn}                 Da de baja un libro                  I

GET     /api/v1/authors/{id}                 Detalle de un autor               S  I
GET     /api/v1/authors/{id}/books           Libros de un autor                S  I

GET     /api/v1/members/{id}                 Detalle de un socio               S  I
POST    /api/v1/members                      Alta de socio
GET     /api/v1/members/{id}/loans           Préstamos de un socio             S  I

POST    /api/v1/loans                        Presta un libro (crea préstamo)
GET     /api/v1/loans/{id}                   Detalle de un préstamo            S  I
PATCH   /api/v1/loans/{id}                   Devuelve el libro                 
                                             cuerpo: {"status": "RETURNED"}

Reglas aplicadas
- Sustantivos en plural, minúsculas y kebab-case. Nunca verbos en la URI.
- El método dice la acción; la URI dice sobre qué recurso.
- Un nivel de anidamiento como mucho: /members/{id}/loans, no
  /members/{id}/loans/{loanId}/book.
- El ISBN es el identificador del libro porque ya es único y estable.
```

**3. URIs mal diseñadas y su corrección.** Son errores reales y frecuentes en proyectos de alumnos (y en APIs profesionales).

`uris-antes-despues.txt`
```text
URIS MAL DISEÑADAS → BIEN DISEÑADAS

Antes (mal)                                   Después (bien)
--------------------------------------------  ------------------------------------
GET  /api/v1/getAllBooks                      GET    /api/v1/books
GET  /api/v1/book/9788437604947               GET    /api/v1/books/9788437604947
POST /api/v1/books/create                     POST   /api/v1/books
POST /api/v1/books/9788437604947/delete       DELETE /api/v1/books/9788437604947
GET  /api/v1/deleteBook?isbn=9788437604947    DELETE /api/v1/books/9788437604947
GET  /api/v1/Members/7/Loans                  GET    /api/v1/members/7/loans
GET  /api/v1/books_by_author/12               GET    /api/v1/authors/12/books
POST /api/v1/returnLoan?id=1024               PATCH  /api/v1/loans/1024
                                                     {"status": "RETURNED"}
GET  /api/v1/books.json                       GET    /api/v1/books
                                                     Accept: application/json

Por qué
- 1, 3, 4, 5 y 8: el verbo ya lo pone el método HTTP.
- 5: un GET nunca cambia nada (es seguro); un buscador o una caché
  podría borrar libros solo con visitarlo.
- 2: las colecciones van en plural, también al pedir un elemento.
- 6: minúsculas siempre (en la ruta, Books y books son URIs distintas).
- 7: los libros de un autor son un subrecurso del autor.
- 9: el formato se negocia con Accept, no con la extensión.
```

**4. Un API propio en tu ordenador.** JSONPlaceholder y DummyJSON no guardan los cambios, así que no sirven para ver qué pasa al repetir una petición. Necesitamos un API que sí los guarde, y lo vamos a levantar nosotros: **json-server** lee un fichero JSON y publica cada lista como una colección REST, con GET, POST, PUT, PATCH y DELETE. Los cambios se escriben en el propio fichero.

`db.json`
```json
{
  "books": [
    {
      "id": "1",
      "isbn": "9788420412146",
      "title": "Don Quijote de la Mancha",
      "available": false
    }
  ]
}
```

Abre un terminal en la carpeta del ejemplo, arranca el servidor y **déjalo abierto** (la primera vez, `npx` pedirá permiso para descargarlo):

`servidor.sh`
```bash
npx json-server db.json
```

✅ **Si ves esto, vas bien:**
```text
JSON Server started on PORT :3000
Press CTRL-C to stop
Watching db.json...

Index:
http://localhost:3000/

Static files:
Serving ./public directory if it exists

Endpoints:
http://localhost:3000/books
```

En otro terminal, `curl http://localhost:3000/books` devuelve la lista. Para pararlo, `Ctrl+C`. Dos detalles de json-server: identifica siempre los elementos por un campo `id` que genera él (en nuestra API será el ISBN), y al arrancar añade al fichero una línea `"$schema"` que puedes ignorar.

**5. La idempotencia, comprobada.** Esta colección de Bruno crea un libro, lo reemplaza dos veces, lo modifica, lo borra dos veces y comprueba en cada paso lo que dice la teoría. La última petición borra un segundo libro de prueba para dejar `db.json` como estaba, así que puedes ejecutarla tantas veces como quieras.

`opencollection.yml`
```yaml
opencollection: 1.0.0

info:
  name: Idempotencia
```

`environments/dev.yml`
```yaml
name: dev
variables:
  - name: baseUrl
    value: http://localhost:3000
```

`01-create.yml`
```yaml
info:
  name: 01 Crear (POST)
  type: http
  seq: 1

http:
  method: POST
  url: "{{baseUrl}}/books"
  body:
    type: json
    data: |-
      {
        "isbn": "9788437604947",
        "title": "Cien años de soledad",
        "available": true
      }

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "201"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.id
        method: jsonq
      variable:
        name: bookId
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`02-create-again.yml`
```yaml
info:
  name: 02 Crear otra vez (POST)
  type: http
  seq: 2

http:
  method: POST
  url: "{{baseUrl}}/books"
  body:
    type: json
    data: |-
      {
        "isbn": "9788437604947",
        "title": "Cien años de soledad",
        "available": true
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("POST no es idempotente: crea OTRO libro con otro id", function() {
          expect(res.body.id).to.not.equal(bru.getVar("bookId"));
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "201"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.id
        method: jsonq
      variable:
        name: secondId
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`03-replace.yml`
```yaml
info:
  name: 03 Reemplazar (PUT)
  type: http
  seq: 3

http:
  method: PUT
  url: "{{baseUrl}}/books/{{bookId}}"
  body:
    type: json
    data: |-
      {
        "isbn": "9788437604947",
        "title": "Cien años de soledad",
        "available": false
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("PUT es idempotente: mismo recurso y mismo estado", function() {
          expect(res.body.id).to.equal(bru.getVar("bookId"));
          expect(res.body.available).to.equal(false);
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`04-replace-again.yml` es igual que `03-replace.yml`, con otro nombre y `seq: 4`.

`05-patch.yml`
```yaml
info:
  name: 05 Modificar un campo (PATCH)
  type: http
  seq: 5

http:
  method: PATCH
  url: "{{baseUrl}}/books/{{bookId}}"
  body:
    type: json
    data: |-
      {
        "title": "Cien años de soledad (edición conmemorativa)"
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("PATCH cambia el título y conserva el resto", function() {
          expect(res.body.title).to.contain("conmemorativa");
          expect(res.body.isbn).to.equal("9788437604947");
          expect(res.body.available).to.equal(false);
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`06-delete.yml`
```yaml
info:
  name: 06 Borrar (DELETE)
  type: http
  seq: 6

http:
  method: DELETE
  url: "{{baseUrl}}/books/{{bookId}}"

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`07-delete-again.yml`
```yaml
info:
  name: 07 Borrar otra vez (DELETE)
  type: http
  seq: 7

http:
  method: DELETE
  url: "{{baseUrl}}/books/{{bookId}}"

runtime:
  scripts:
    - type: tests
      code: |-
        test("el estado no cambia: el libro sigue sin existir", function() {
          expect(res.body.error).to.equal("Not Found");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "404"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`08-get-deleted.yml`
```yaml
info:
  name: 08 Consultar el borrado (GET)
  type: http
  seq: 8

http:
  method: GET
  url: "{{baseUrl}}/books/{{bookId}}"

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "404"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`09-delete-second.yml`
```yaml
info:
  name: 09 Limpiar el segundo libro (DELETE)
  type: http
  seq: 9

http:
  method: DELETE
  url: "{{baseUrl}}/books/{{secondId}}"

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

Con json-server arrancado, ejecuta la colección con el Runner (o `bru run --env dev` desde su carpeta).

✅ **Si ves esto, vas bien:**
```text
01-create (201 Created) - 30 ms
Assertions
   ✓ res.status: eq 201
02-create-again (201 Created) - 9 ms
Tests
   ✓ POST no es idempotente: crea OTRO libro con otro id
Assertions
   ✓ res.status: eq 201
03-replace (200 OK) - 6 ms
Tests
   ✓ PUT es idempotente: mismo recurso y mismo estado
Assertions
   ✓ res.status: eq 200
04-replace-again (200 OK) - 6 ms
Tests
   ✓ PUT es idempotente: mismo recurso y mismo estado
Assertions
   ✓ res.status: eq 200
05-patch (200 OK) - 6 ms
Tests
   ✓ PATCH cambia el título y conserva el resto
Assertions
   ✓ res.status: eq 200
06-delete (200 OK) - 11 ms
Assertions
   ✓ res.status: eq 200
07-delete-again (404 Not Found) - 6 ms
Tests
   ✓ el estado no cambia: el libro sigue sin existir
Assertions
   ✓ res.status: eq 404
08-get-deleted (404 Not Found) - 6 ms
Assertions
   ✓ res.status: eq 404
09-delete-second (200 OK) - 7 ms
Assertions
   ✓ res.status: eq 200
📊 Execution Summary
┌───────────────┬──────────────┐
│ Metric        │    Result    │
├───────────────┼──────────────┤
│ Status        │    ✓ PASS    │
├───────────────┼──────────────┤
│ Requests      │ 9 (9 Passed) │
├───────────────┼──────────────┤
│ Tests         │     5/5      │
├───────────────┼──────────────┤
│ Assertions    │     9/9      │
├───────────────┼──────────────┤
│ Duration (ms) │     1437     │
└───────────────┴──────────────┘
```

Lo que demuestra cada paso:
- **01 y 02**: dos POST idénticos crean **dos** libros con identificadores distintos. POST no es idempotente.
- **03 y 04**: dos PUT idénticos dejan el mismo libro en el mismo estado. PUT es idempotente.
- **05**: PATCH cambia solo `title` y conserva el resto.
- **06 y 07**: el primer DELETE borra (200); el segundo responde 404, pero el estado es el mismo: el libro no existe. DELETE es idempotente.

Abre `db.json` entre una petición y otra (en el Runner, ejecútalas de una en una) y verás cómo cambia. json-server responde 201 al crear, pero sin cabecera `Location`, y 200 con el libro borrado al hacer DELETE. En nuestra API, 201 irá con `Location` y el DELETE responderá 204 sin cuerpo (03.03).

| Error frecuente | Causa | Solución |
|---|---|---|
| `GET /api/v1/deleteBook?isbn=…` | Usar GET para cambiar algo | GET siempre es seguro; para borrar, `DELETE /api/v1/books/{isbn}` |
| `POST /api/v1/books/update` | Verbos en la URI | El método dice la acción: `PUT` o `PATCH /api/v1/books/{isbn}` |
| Se usa PUT enviando solo el campo que cambia y los demás quedan a `null` | PUT reemplaza el recurso entero | Para cambiar algunos campos, PATCH |
| La colección de Bruno falla en `03-replace` con `404` | Has ejecutado esa petición sola: `{{bookId}}` aún no tiene valor | Ejecuta la colección completa en orden con el Runner |
| `connect ECONNREFUSED 127.0.0.1:3000` en Bruno | json-server no está arrancado o lo has cerrado | Arráncalo en otro terminal desde la carpeta de `db.json` y déjalo abierto |
| `500 Internal Server Error` al crear un libro con json-server | El cuerpo no es JSON válido (por ejemplo, falta cerrar una llave) | Revisa el JSON (02.04). Un buen API respondería 400, no 500 (03.03) |

### Práctica

1. **Endpoints del gimnasio.** A partir de tu `recursos-gimnasio.txt` (03.01), escribe `endpoints-gimnasio.txt` con el formato de `endpoints-biblioteca.txt`: método, URI, qué hace y si es seguro e idempotente. Incluye cómo se **reserva** plaza y cómo se **cancela** una reserva.
2. **Corrige estas URIs** en `uris-corregidas.txt` y explica qué estaba mal en cada una:

   ```text
   GET  /api/v1/getClasses
   POST /api/v1/class/new
   GET  /api/v1/Classes/31
   POST /api/v1/bookings/88/cancel
   GET  /api/v1/removeMember?id=7
   GET  /api/v1/bookings_of_member/7
   PUT  /api/v1/members/7/updateEmail
   ```

3. **API de reservas con json-server.** En una carpeta nueva, crea un `db.json` con una colección `bookings` vacía y arranca json-server con él.
4. **Colección `Reservas` en Bruno**, con entorno `dev` y `baseUrl` = `http://localhost:3000`. Cada reserva lleva `classId`, `memberId` y `status`:
   1. **Reservar** (POST `/bookings`) en la clase 31 para el socio 7 con `status` = `CONFIRMED`. Comprueba el código y guarda el `id` en `bookingId`.
   2. **Consultar** la reserva y comprobar en un test que está `CONFIRMED`.
   3. **Cancelar** con PATCH, enviando solo `status` = `CANCELLED`. Comprueba que queda cancelada y que conserva la clase.
   4. **Cancelar otra vez**, con la misma petición. Comprueba que es la misma reserva y sigue cancelada.
   5. **Borrar** y **borrar otra vez**. Comprueba los códigos y, en el segundo, el mensaje de error.
5. Responde:
   1. El apartado 4.4 muestra que ese PATCH es idempotente. Pon un ejemplo de PATCH sobre una clase que **no** lo sea.
   2. ¿Por qué cancelar es un PATCH y no un DELETE de la reserva? ¿Qué se perdería con DELETE?
   3. Un socio pulsa dos veces "Reservar" porque la app tarda. ¿Qué pasa con tu diseño? Propón dos formas de evitar la reserva duplicada.

> **Pista:** parte de la colección `Idempotencia` del ejemplo: cambia `books` por `bookings`, los cuerpos y los tests. Para comprobar un número en un test, `expect(res.body.classId).to.equal(31)`.

### Solución

**1. Endpoints.**

`endpoints-gimnasio.txt`
```text
ENDPOINTS · API del gimnasio (v1)                S = seguro · I = idempotente

Método  URI                                    Qué hace                        S  I
------  -------------------------------------  ------------------------------  -  -
GET     /api/v1/classes                        Lista las clases                S  I
GET     /api/v1/classes/{id}                   Detalle de una clase            S  I
POST    /api/v1/classes                        Crea una clase
PUT     /api/v1/classes/{id}                   Reemplaza una clase                I
DELETE  /api/v1/classes/{id}                   Elimina una clase                  I
GET     /api/v1/classes/{id}/bookings          Reservas de una clase           S  I

GET     /api/v1/trainers/{id}                  Detalle de un monitor           S  I
GET     /api/v1/trainers/{id}/classes          Clases de un monitor            S  I

POST    /api/v1/members                        Alta de socio
GET     /api/v1/members/{id}                   Detalle de un socio             S  I
PATCH   /api/v1/members/{id}                   Cambia datos del socio
GET     /api/v1/members/{id}/bookings          Reservas de un socio            S  I

POST    /api/v1/bookings                       Reserva plaza en una clase
                                               cuerpo: {"classId": 31, "memberId": 7}
GET     /api/v1/bookings/{id}                  Detalle de una reserva          S  I
PATCH   /api/v1/bookings/{id}                  Cancela la reserva                 I(*)
                                               cuerpo: {"status": "CANCELLED"}

(*) Este PATCH concreto es idempotente porque fija un valor absoluto:
    repetirlo deja la reserva igual de cancelada. Un PATCH como
    {"plazas": "+1"} no lo sería. Por eso PATCH es "no necesariamente".
```

**2. URIs corregidas.**

`uris-corregidas.txt`
```text
URIS DE LA PRÁCTICA, CORREGIDAS

Enunciado                                     Corregida
--------------------------------------------  ------------------------------------
GET  /api/v1/getClasses                       GET    /api/v1/classes
POST /api/v1/class/new                        POST   /api/v1/classes
GET  /api/v1/Classes/31                       GET    /api/v1/classes/31
POST /api/v1/bookings/88/cancel               PATCH  /api/v1/bookings/88
                                                     {"status": "CANCELLED"}
GET  /api/v1/removeMember?id=7                DELETE /api/v1/members/7
GET  /api/v1/bookings_of_member/7             GET    /api/v1/members/7/bookings
PUT  /api/v1/members/7/updateEmail            PATCH  /api/v1/members/7
                                                     {"email": "nuevo@example.com"}

Errores corregidos
- Verbos en la URI (getClasses, new, cancel, removeMember, updateEmail).
- Singular (class) y mayúsculas (Classes).
- Un GET que borra: el más grave, porque GET debe ser seguro.
- PUT para cambiar un solo campo: PUT reemplaza el recurso entero; aquí
  corresponde PATCH.
- Relación expresada con guion bajo en vez de subrecurso.
```

**3. API de reservas.**

`db.json`
```json
{
  "bookings": []
}
```

`servidor.sh`
```bash
npx json-server db.json
```

```text
JSON Server started on PORT :3000
Press CTRL-C to stop
Watching db.json...

Index:
http://localhost:3000/

Static files:
Serving ./public directory if it exists

Endpoints:
http://localhost:3000/bookings
```

**4. Colección `Reservas`.**

`opencollection.yml`
```yaml
opencollection: 1.0.0

info:
  name: Reservas
```

`environments/dev.yml`
```yaml
name: dev
variables:
  - name: baseUrl
    value: http://localhost:3000
```

`01-create-booking.yml`
```yaml
info:
  name: 01 Reservar plaza (POST)
  type: http
  seq: 1

http:
  method: POST
  url: "{{baseUrl}}/bookings"
  body:
    type: json
    data: |-
      {
        "classId": 31,
        "memberId": 7,
        "status": "CONFIRMED"
      }

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "201"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.id
        method: jsonq
      variable:
        name: bookingId
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`02-get-booking.yml`
```yaml
info:
  name: 02 Consultar la reserva (GET)
  type: http
  seq: 2

http:
  method: GET
  url: "{{baseUrl}}/bookings/{{bookingId}}"

runtime:
  scripts:
    - type: tests
      code: |-
        test("la reserva está confirmada", function() {
          expect(res.body.status).to.equal("CONFIRMED");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`03-cancel.yml`
```yaml
info:
  name: 03 Cancelar (PATCH)
  type: http
  seq: 3

http:
  method: PATCH
  url: "{{baseUrl}}/bookings/{{bookingId}}"
  body:
    type: json
    data: |-
      {
        "status": "CANCELLED"
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("queda cancelada y conserva la clase y el socio", function() {
          expect(res.body.status).to.equal("CANCELLED");
          expect(res.body.classId).to.equal(31);
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`04-cancel-again.yml`
```yaml
info:
  name: 04 Cancelar otra vez (PATCH)
  type: http
  seq: 4

http:
  method: PATCH
  url: "{{baseUrl}}/bookings/{{bookingId}}"
  body:
    type: json
    data: |-
      {
        "status": "CANCELLED"
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("sigue cancelada y es la misma reserva", function() {
          expect(res.body.id).to.equal(bru.getVar("bookingId"));
          expect(res.body.status).to.equal("CANCELLED");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`05-delete.yml`
```yaml
info:
  name: 05 Borrar (DELETE)
  type: http
  seq: 5

http:
  method: DELETE
  url: "{{baseUrl}}/bookings/{{bookingId}}"

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`06-delete-again.yml`
```yaml
info:
  name: 06 Borrar otra vez (DELETE)
  type: http
  seq: 6

http:
  method: DELETE
  url: "{{baseUrl}}/bookings/{{bookingId}}"

runtime:
  scripts:
    - type: tests
      code: |-
        test("explica que ya no existe", function() {
          expect(res.body.error).to.equal("Not Found");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "404"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

Ejecución completa (`bru run --env dev`):
```text
01-create-booking (201 Created) - 26 ms
Assertions
   ✓ res.status: eq 201
02-get-booking (200 OK) - 5 ms
Tests
   ✓ la reserva está confirmada
Assertions
   ✓ res.status: eq 200
03-cancel (200 OK) - 8 ms
Tests
   ✓ queda cancelada y conserva la clase y el socio
Assertions
   ✓ res.status: eq 200
04-cancel-again (200 OK) - 8 ms
Tests
   ✓ sigue cancelada y es la misma reserva
Assertions
   ✓ res.status: eq 200
05-delete (200 OK) - 8 ms
Assertions
   ✓ res.status: eq 200
06-delete-again (404 Not Found) - 5 ms
Tests
   ✓ explica que ya no existe
Assertions
   ✓ res.status: eq 404
📊 Execution Summary
┌───────────────┬──────────────┐
│ Metric        │    Result    │
├───────────────┼──────────────┤
│ Status        │    ✓ PASS    │
├───────────────┼──────────────┤
│ Requests      │ 6 (6 Passed) │
├───────────────┼──────────────┤
│ Tests         │     4/4      │
├───────────────┼──────────────┤
│ Assertions    │     6/6      │
├───────────────┼──────────────┤
│ Duration (ms) │     1060     │
└───────────────┴──────────────┘
```

**5. Preguntas.**
1. Un PATCH con una operación relativa: `{"bookedSpots": "+1"}` o, en general, cualquiera que calcule el nuevo valor a partir del anterior. Repetirlo suma otra plaza. Por eso es mejor diseñar los PATCH con **valores absolutos**.
2. Con DELETE la reserva desaparecería: no quedaría constancia de que existió ni de cuándo se canceló, y no se podría consultar "mis reservas canceladas" ni sacar estadísticas. Cancelar es un **cambio de estado**; la reserva sigue existiendo. DELETE se reserva para cuando de verdad se quiere eliminar el recurso.
3. Con `POST /api/v1/bookings` se crearían **dos** reservas, porque POST no es idempotente. Dos soluciones: una **regla de negocio** que rechace una segunda reserva del mismo socio en la misma clase (409 Conflict, 03.03), y la cabecera **`Idempotency-Key`** para que el servidor reconozca el reintento y devuelva la reserva ya creada. Además, la app debería desactivar el botón mientras espera.

---

## 03.03 · Códigos de estado y formato de errores (RFC 9457)

### Resumen
- El código de estado es **lo primero que mira el cliente**: decide si la operación ha ido bien sin leer el cuerpo. Tiene que ser el correcto.
- Éxito: **200** (con cuerpo), **201** + `Location` (creado), **204** (sin cuerpo). Error del cliente: **400**, **401**, **403**, **404**, **405**, **409**, **415**, **422**. Error del servidor: **500**, **503**.
- Una búsqueda sin resultados es **200 con lista vacía**, no 404. Un 500 nunca es una respuesta "a propósito".
- Cada API inventa su formato de error. El estándar **RFC 9457** (*Problem Details for HTTP APIs*) lo unifica: tipo `application/problem+json` con `type`, `title`, `status`, `detail` e `instance`, más los campos propios que haga falta.
- Spring Boot genera estos errores con `ProblemDetail` (07.05). Aquí decidimos cuáles devuelve nuestra API y cómo son.

### Teoría

**Elegir el código: preguntas en orden.**
1. ¿Ha ido bien? → **2xx**. ¿Hay cuerpo? 200. ¿Se ha creado algo? 201 con `Location`. ¿No hay nada que devolver? 204.
2. ¿Falta la identificación o no es válida? → **401**.
3. ¿Sé quién eres, pero no puedes hacerlo? → **403**.
4. ¿El recurso de la URI no existe? → **404**. ¿Existe, pero no admite ese método? → **405** con `Allow`.
5. ¿La petición está mal formada o los datos no son válidos (falta un campo, un ISBN con letras)? → **400**.
6. ¿Los datos son válidos, pero chocan con el estado actual (ISBN repetido, no quedan ejemplares, el socio ya tiene 3 préstamos)? → **409**.
7. ¿El cuerpo no viene en un formato que entienda (XML en lugar de JSON)? → **415**. ¿El cliente pide un formato que no produzco? → **406**.
8. ¿Ha fallado algo mío que no esperaba? → **500**. ¿Estoy temporalmente sin servicio (la base de datos no responde)? → **503**.

**Decisiones del curso sobre los casos dudosos.**

| Situación | Decisión | Motivo |
|---|---|---|
| Datos no válidos (Bean Validation) | 400 | Es lo que devuelve Spring Boot al validar con `@Valid` (07.01). Algunas APIs usan 422; sé consistente |
| Regla de negocio incumplida | 409 | El problema no está en la petición, sino en el estado del recurso |
| Búsqueda o filtro sin resultados | 200 con `[]` o `content` vacío | La colección existe; simplemente está vacía |
| Borrado correcto | 204 sin cuerpo | No hay nada que devolver |
| PUT o PATCH correctos | 200 con el recurso | Evita que el cliente tenga que hacer un GET para ver cómo ha quedado |
| Recurso de otro usuario | 403 (o 404 para no revelar que existe) | 03.06 |

**401 frente a 403**, repaso de 02.03: 401 es "no sé quién eres" (falta el token o no es válido) y debe ir con la cabecera `WWW-Authenticate`; 403 es "sé quién eres, pero no tienes permiso".

**Errores que no debes cometer.**
- **200 con `{"error": "..."}`** en el cuerpo. El cliente cree que ha ido bien.
- **404 para una búsqueda vacía.** El cliente no distingue "no hay resultados" de "esa URL no existe".
- **500 por datos mal enviados.** Si el cliente envía un token con mal formato y tu código lanza una excepción no controlada, el cliente recibe 500 y cree que el servidor está roto. Es culpa del cliente: 400 o 401.
- **400 para todo.** Pierdes información: el cliente no sabe si reintentar, corregir o pedir permiso.

**El problema: cada API, un formato.** En el ejemplo verás que cinco APIs públicas devuelven cinco formatos de error distintos: `message`, `error`, `reason`, con o sin `status`… Un cliente que use varias necesita una forma de leer los errores por cada una. Por eso el IETF publicó en 2016 el **RFC 7807**, sustituido en 2023 por el **RFC 9457**.

**Problem Details (RFC 9457).** El error va con `Content-Type: application/problem+json` y un objeto con estos campos, todos opcionales:

| Campo | Qué es | Ejemplo |
|---|---|---|
| `type` | URI que identifica el **tipo** de problema; puede llevar a su documentación. Si no hay tipo propio, `about:blank` | `https://api.biblioteca.example/problems/duplicate-isbn` |
| `title` | Resumen corto del tipo, el mismo para todos los problemas de ese tipo | `ISBN repetido` |
| `status` | El código HTTP, repetido para quien solo tenga el cuerpo | `409` |
| `detail` | Explicación de **este** caso concreto, para personas | `Ya existe un libro con ISBN 9788437604947` |
| `instance` | URI de la petición o del caso concreto | `/api/v1/books` |
| (extensiones) | Campos propios que necesite el cliente | `errors`, `memberId`, `maxLoans` |

Reglas de uso:
- El cliente decide **qué hacer** con `status` y `type`, nunca analizando el texto de `detail`.
- `type` = `about:blank` significa "nada más que el código HTTP": entonces `title` es el texto estándar del código (`Not Found`).
- `detail` explica el problema al cliente, pero **nunca** expone detalles internos: nada de trazas de excepciones, SQL ni rutas del servidor.
- Las extensiones permiten dar datos útiles: la lista de campos con errores en una validación, o cuántos préstamos tiene el socio.

Spring Boot tiene la clase `ProblemDetail` que produce exactamente este formato. En 07.05 la usarás desde un `@RestControllerAdvice` para que **todos** los errores de tu API salgan así.

### Código de ejemplo

**1 a 5. Cinco APIs, cinco formatos de error.**

`01-pokeapi.sh`
```bash
curl -i https://pokeapi.co/api/v2/pokemon/noexiste
```

```text
HTTP/2 404 
date: Mon, 02 Nov 2026 09:12:30 GMT
content-type: application/json; charset=utf-8

{"status":404,"message":"Not Found"}
```

`02-rickandmorty.sh`
```bash
curl -i https://rickandmortyapi.com/api/character/9999
```

```text
HTTP/2 404 
date: Mon, 02 Nov 2026 09:12:31 GMT
content-type: application/json; charset=utf-8

{"error":"Character not found"}
```

`03-open-meteo.sh`
```bash
curl -i "https://api.open-meteo.com/v1/forecast?latitude=140&longitude=-3.7&current=temperature_2m"
```

```text
HTTP/1.1 400 Bad Request
Date: Mon, 02 Nov 2026 09:12:32 GMT
Content-Type: application/json; charset=utf-8

{"error":true,"reason":"Latitude must be in range of -90 to 90°. Given: 140.0."}
```

`04-frankfurter.sh`
```bash
curl -i https://api.frankfurter.dev/v2/rate/eur/xxx
```

```text
HTTP/2 422 
date: Mon, 02 Nov 2026 09:12:34 GMT
content-type: application/json; charset=utf-8

{"status":422,"message":"invalid currency: XXX"}
```

`05-dummyjson.sh`
```bash
curl -i https://dummyjson.com/auth/me -H "Authorization: Bearer abc.def.ghi"
```

```text
HTTP/2 500 
date: Mon, 02 Nov 2026 09:12:35 GMT
content-type: application/json; charset=utf-8
x-ratelimit-limit: 100
x-ratelimit-remaining: 99

{"message":"invalid token"}
```

Qué observar:
- Los códigos de los cuatro primeros son correctos: 404 si no existe, 400 si el dato no es válido, 422 si la moneda no existe (Frankfurter usa 422 para datos válidos en forma, pero imposibles).
- Los cuerpos son todos distintos: `status` + `message`, `error`, `error` + `reason`… Ninguno usa `application/problem+json`.
- DummyJSON responde **500** a un token con forma de JWT, pero inválido. Es un error del cliente; lo correcto sería 401. Con un token que no tiene esa forma responde 401 (lo verás en la práctica): el código cambia según cómo falla su programa, no según el problema. Es lo que pasa cuando las excepciones no se gestionan.

**6. Los códigos de la biblioteca.** Decididos operación por operación:

`codigos-biblioteca.txt`
```text
CÓDIGOS DE ESTADO · API de la biblioteca (v1)

Operación                         Éxito                    Errores posibles
--------------------------------  -----------------------  ------------------------------
GET    /books                     200 (lista, quizá vacía) 400 parámetro no válido
GET    /books/{isbn}              200                      404 no existe
POST   /books                     201 + Location           400 datos no válidos
                                                           409 ISBN repetido
                                                           415 cuerpo que no es JSON
PUT    /books/{isbn}              200 (con el libro)       400, 404
PATCH  /books/{isbn}              200 (con el libro)       400, 404
DELETE /books/{isbn}              204 (sin cuerpo)         404 no existe
                                                           409 tiene préstamos activos
POST   /loans                     201 + Location           400 faltan datos
                                                           404 socio o libro no existen
                                                           409 sin ejemplares disponibles
                                                           409 socio con 3 préstamos
PATCH  /loans/{id} (devolver)     200                      404, 409 ya estaba devuelto

En todas: 401 sin token o token no válido (módulo 11)
          403 con token, pero sin permiso (p. ej. un socio que borra libros)
          500 fallo no controlado (nunca a propósito)
Todos los errores 4xx y 5xx: cuerpo application/problem+json (RFC 9457).
```

**7. Los errores de la biblioteca en formato RFC 9457.**

Libro que no existe: sin tipo propio, basta con `about:blank`.

`problem-404-book.json`
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "No existe ningún libro con ISBN 9780000000000",
  "instance": "/api/v1/books/9780000000000"
}
```

Error de validación, con la extensión `errors` (la lista de campos). Es el formato que montarás en 07.05.

`problem-400-validation.json`
```json
{
  "type": "https://api.biblioteca.example/problems/validation-error",
  "title": "Datos no válidos",
  "status": 400,
  "detail": "La petición tiene 2 campos con errores",
  "instance": "/api/v1/books",
  "errors": [
    { "field": "isbn", "message": "debe tener 13 dígitos" },
    { "field": "title", "message": "no puede estar vacío" }
  ]
}
```

ISBN repetido, con tipo propio:

`problem-409-isbn.json`
```json
{
  "type": "https://api.biblioteca.example/problems/duplicate-isbn",
  "title": "ISBN repetido",
  "status": 409,
  "detail": "Ya existe un libro con ISBN 9788437604947",
  "instance": "/api/v1/books"
}
```

Regla de negocio, con extensiones que ayudan al cliente a explicar el error:

`problem-409-loan-limit.json`
```json
{
  "type": "https://api.biblioteca.example/problems/loan-limit-reached",
  "title": "Límite de préstamos alcanzado",
  "status": 409,
  "detail": "El socio 7 ya tiene 3 préstamos activos (máximo: 3)",
  "instance": "/api/v1/loans",
  "memberId": 7,
  "activeLoans": 3,
  "maxLoans": 3
}
```

Comprobación de que los cuatro son JSON válidos (con el validador de 02.04):

```text
problem-400-validation.json: JSON válido
problem-404-book.json: JSON válido
problem-409-isbn.json: JSON válido
problem-409-loan-limit.json: JSON válido
```

| Error frecuente | Causa | Solución |
|---|---|---|
| Respuesta `200` con `{"error": "No existe"}` | Elegir el código sin pensar en el cliente | El código dice si ha ido bien: 404 |
| `404` al buscar y no encontrar nada | Confundir "colección vacía" con "recurso inexistente" | 200 con la lista vacía |
| `500` cuando el cliente envía datos incorrectos | Una excepción no controlada llega al cliente | Valida y devuelve 400, 401 o 409; el 500 solo para lo inesperado |
| `detail` con `java.lang.NullPointerException at com.joyfe…` | Se copia el mensaje de la excepción al error | `detail` habla al cliente; la traza va al log del servidor |
| `Content-Type: application/json` en un error RFC 9457 | Olvidar el tipo propio de los problemas | `application/problem+json` (Spring lo pone solo, 07.05) |

### Práctica

1. **Códigos del gimnasio.** Escribe en `codigos-gimnasio.txt` el código de estado de cada situación:
   1. Se crea una reserva correctamente.
   2. Se pide la clase 999, que no existe.
   3. Se reserva en una clase sin plazas libres.
   4. Se crea una clase con `"capacity": -5`.
   5. Se cancela una reserva correctamente (PATCH).
   6. Se borra una clase correctamente.
   7. Un socio sin token pide sus reservas.
   8. Un socio con token intenta borrar una clase.
   9. Se envía el cuerpo en XML.
   10. Se buscan clases de "zumba" y no hay ninguna.
   11. Se hace DELETE sobre `/api/v1/classes` (la colección entera).
   12. La base de datos no responde.
2. **Problem Details.** Escribe en formato RFC 9457 los errores de las situaciones 3 (`problem-class-full.json`, con extensiones útiles para el cliente), 4 (`problem-validation.json`) y 8 (`problem-forbidden.json`). Valida que son JSON correcto (02.04).
3. **Auditoría de APIs reales.** Con curl, comprueba qué responde cada API y decide si el código es correcto:
   1. Login de DummyJSON (`POST /auth/login`) con la usuaria `emilys` y una contraseña equivocada (cuerpo en `credenciales-malas.json`).
   2. `GET /auth/me` de DummyJSON con la cabecera `Authorization: Bearer token-inventado`.
   3. Búsqueda en Rick and Morty de personajes con nombre `zzzz` (`/api/character/?name=zzzz`).
   4. Búsqueda en DummyJSON de productos con `q=zzzz` (`/products/search?q=zzzz&select=title`).
4. Responde:
   1. En 3.1, ¿qué código debería devolver DummyJSON y qué cabecera le falta?
   2. Compara 3.2 con el ejemplo 5 de esta sección. ¿Qué te dice que el mismo endpoint devuelva 401 o 500 según el token?
   3. ¿Cuál de las dos búsquedas vacías (3.3 y 3.4) está bien diseñada y por qué?

### Solución

**1. Códigos.**

`codigos-gimnasio.txt`
```text
CÓDIGOS DE ESTADO · API del gimnasio · soluciones

 #  Situación                                              Código
--  -----------------------------------------------------  ---------------------------
 1  Se crea una reserva correctamente                      201 Created + Location
 2  Se pide la clase 999, que no existe                    404 Not Found
 3  Se reserva en una clase sin plazas libres              409 Conflict
 4  Se crea una clase con "capacity": -5                   400 Bad Request
 5  Se cancela una reserva correctamente (PATCH)           200 OK (con la reserva)
 6  Se borra una clase correctamente                       204 No Content
 7  Un socio sin token pide sus reservas                   401 Unauthorized
 8  Un socio con token intenta borrar una clase            403 Forbidden
 9  Se envía el cuerpo en XML                              415 Unsupported Media Type
10  Se buscan clases de "zumba" y no hay ninguna           200 OK con lista vacía
11  Se hace DELETE sobre /api/v1/classes (la colección)    405 Method Not Allowed + Allow
12  La base de datos no responde                           503 Service Unavailable
                                                           (500 si es un fallo no previsto)
```

**2. Problem Details.**

`problem-class-full.json`
```json
{
  "type": "https://api.gimnasio.example/problems/class-full",
  "title": "Clase completa",
  "status": 409,
  "detail": "La clase 31 (Spinning, 02/11/2026 18:00) no tiene plazas libres",
  "instance": "/api/v1/bookings",
  "classId": 31,
  "capacity": 20
}
```

`problem-validation.json`
```json
{
  "type": "https://api.gimnasio.example/problems/validation-error",
  "title": "Datos no válidos",
  "status": 400,
  "detail": "La petición tiene 1 campo con errores",
  "instance": "/api/v1/classes",
  "errors": [
    { "field": "capacity", "message": "debe ser mayor que 0" }
  ]
}
```

`problem-forbidden.json`
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Solo el personal del gimnasio puede borrar clases",
  "instance": "/api/v1/classes/31"
}
```

```text
credenciales-malas.json: JSON válido
problem-class-full.json: JSON válido
problem-forbidden.json: JSON válido
problem-validation.json: JSON válido
```

**3. Auditoría.**

`credenciales-malas.json`
```json
{
  "username": "emilys",
  "password": "contraseña-equivocada"
}
```

`01-login-mal.sh`
```bash
curl -i -X POST https://dummyjson.com/auth/login -H "Content-Type: application/json" --data @credenciales-malas.json
```

```text
HTTP/2 400 
date: Mon, 02 Nov 2026 09:12:36 GMT
content-type: application/json; charset=utf-8
x-ratelimit-limit: 100
x-ratelimit-remaining: 98

{"message":"Invalid credentials"}
```

`02-token-roto.sh`
```bash
curl -i https://dummyjson.com/auth/me -H "Authorization: Bearer token-inventado"
```

```text
HTTP/2 401 
date: Mon, 02 Nov 2026 09:12:38 GMT
content-type: application/json; charset=utf-8
x-ratelimit-limit: 100
x-ratelimit-remaining: 97

{"message":"Invalid/Expired Token!"}
```

`03-busqueda-vacia.sh`
```bash
curl -i "https://rickandmortyapi.com/api/character/?name=zzzz"
```

```text
HTTP/2 404 
date: Mon, 02 Nov 2026 09:12:39 GMT
content-type: application/json; charset=utf-8

{"error":"There is nothing here"}
```

`04-busqueda-vacia-dummyjson.sh`
```bash
curl -i "https://dummyjson.com/products/search?q=zzzz&select=title"
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:12:40 GMT
content-type: application/json; charset=utf-8
x-ratelimit-limit: 100
x-ratelimit-remaining: 99

{"products":[],"total":0,"skip":0,"limit":0}
```

**4. Preguntas.**
1. **401 Unauthorized**, porque las credenciales no son válidas, con la cabecera `WWW-Authenticate` que indica cómo autenticarse. DummyJSON devuelve 400, que da a entender que la petición está mal formada, y no envía `WWW-Authenticate`.
2. Que el código depende de **en qué punto falla su programa**, no del problema: con un texto cualquiera lo detecta y responde 401; con algo que parece un JWT intenta comprobar la firma, salta una excepción no controlada y sale 500. Para el cliente, los dos casos son el mismo: token no válido → 401. Es el motivo de tener un **gestor global de errores** (07.05).
3. La de DummyJSON: 200 con `"products": []` y `"total": 0`. La colección existe; solo está vacía. Rick and Morty responde 404 con `There is nothing here`, y el cliente no puede distinguirlo de una URL mal escrita.

---

## 03.04 · Filtros, ordenación y paginación

### Resumen
- Las colecciones se consultan con **parámetros de consulta**: filtros (`?available=true`), búsqueda (`?q=`), orden (`?sort=title,asc`) y página (`?page=0&size=20`).
- Hay tres formas de paginar: **offset/limit** (posición y cantidad), **page/size** (número de página y tamaño) y **cursor** (un marcador opaco que da el servidor). Las dos primeras son equivalentes; el cursor es para colecciones enormes o que cambian mucho.
- Una respuesta paginada lleva **metadatos** (total, páginas, página actual) y a veces **enlaces** a la siguiente y la anterior, en el cuerpo o en la cabecera `Link`.
- Nunca se devuelve una colección entera sin límite: el servidor fija un tamaño por defecto y uno máximo.
- La biblioteca sigue la convención de **Spring Data** (`page`, `size`, `sort`), que es la que implementarás en 10.05.

### Teoría

**Filtros.** Un filtro es un parámetro con el nombre del campo: `?genre=novela&available=true`. Varios filtros se combinan con **Y**. Para otros casos hay convenciones:

| Necesidad | Convención habitual | Ejemplo |
|---|---|---|
| Igualdad | Nombre del campo | `?status=ACTIVE` |
| Varios valores (O) | Repetir el parámetro o separar con comas | `?genre=novela&genre=poesia`, `?genre=novela,poesia` |
| Rangos | Prefijo o sufijo `min`/`max`, `from`/`to` | `?minPages=100&maxPages=300`, `?from=2026-11-01` |
| Búsqueda libre | `q` o `search` | `?q=soledad` |
| Campos de la respuesta | `fields` o `select` | `?select=title,price` (DummyJSON) |

Documenta qué filtros admite cada colección y qué pasa con un filtro desconocido (lo más seguro: 400).

**Ordenación.** Lo más común es un parámetro `sort` con el campo y la dirección. Spring Data usa `sort=campo,asc|desc` y admite repetirlo para varios criterios: `?sort=publishedYear,desc&sort=title,asc`. Otras APIs usan dos parámetros (`sortBy=price&order=desc`, DummyJSON) o un signo (`sort=-price`). Ordena siempre por algo: sin orden, la misma página puede devolver elementos distintos en dos peticiones.

**Paginación.**

| Estilo | Parámetros | Quién lo usa | Ventajas | Inconvenientes |
|---|---|---|---|---|
| offset/limit | `offset=40&limit=20` (o `skip`) | PokeAPI, DummyJSON | Sencillo; saltar a cualquier posición | Con muchos datos, la base de datos recorre todo lo saltado; si se insertan elementos, se repiten o se pierden |
| page/size | `page=2&size=20` | Spring Data, Rick and Morty | Igual que el anterior, más natural para el usuario | Los mismos |
| Cursor | `after=eyJpZCI6...&limit=20` | APIs grandes (redes sociales, pagos) | Rápido y estable aunque cambien los datos | No permite saltar a la página 7; el cliente solo puede ir hacia delante o atrás |

Equivalencia: con tamaño `size`, la página `page` (empezando en 0) empieza en `offset = page × size`. La página 3 de 10 en 10 (`page=2`) es `offset=20&limit=10`.

**Qué devuelve una respuesta paginada.** Además de los elementos, lo que el cliente necesita para pintar un paginador:
- **En el cuerpo**: total de elementos, total de páginas, página actual y tamaño. A veces también los enlaces `next` y `prev` (PokeAPI, Rick and Morty).
- **En la cabecera `Link`** (RFC 8288): los enlaces con su relación (`rel="next"`, `rel="last"`). Es lo que hace GitHub. Así el cuerpo queda solo para los datos.

En la biblioteca usamos el formato de Spring Data (`PagedModel`), con los elementos en `content` y los metadatos en `page`. Es lo que devolverá tu API en 10.05 sin escribir nada especial.

**Límites: protege tu servidor.** Si `size` no tiene máximo, alguien puede pedir `?size=1000000` y tumbar el servidor. Es el riesgo **API4 (Unrestricted Resource Consumption)** del OWASP API Security Top 10 (03.06). Regla: tamaño por defecto (20) y máximo (100). Spring Data aplica por defecto un máximo de 2000, que se puede bajar en la configuración.

### Código de ejemplo

**1. offset/limit con enlaces en el cuerpo (PokeAPI).**

`01-pokeapi-offset.sh`
```bash
curl "https://pokeapi.co/api/v2/pokemon?limit=3&offset=3"
```

```text
{"count":1351,"next":"https://pokeapi.co/api/v2/pokemon?offset=6&limit=3","previous":"https://pokeapi.co/api/v2/pokemon?offset=0&limit=3","results":[{"name":"charmander","url":"https://pokeapi.co/api/v2/pokemon/4/"},{"name":"charmeleon","url":"https://pokeapi.co/api/v2/pokemon/5/"},{"name":"charizard","url":"https://pokeapi.co/api/v2/pokemon/6/"}]}
```

`count` es el total; `next` y `previous` son las URLs de las páginas vecinas, ya construidas. Con `offset=3` empiezan en el cuarto Pokémon.

**2. Paginación, orden y selección de campos (DummyJSON).**

`02-dummyjson-orden.sh`
```bash
curl "https://dummyjson.com/products?limit=3&skip=0&select=title,price&sortBy=price&order=desc"
```

```text
{"products":[{"id":170,"title":"Durango SXT RWD","price":36999.99},{"id":168,"title":"Charger SXT RWD","price":32999.99},{"id":171,"title":"Pacifica Touring","price":31999.99}],"total":194,"skip":0,"limit":3}
```

Los tres productos más caros de los 194. DummyJSON usa `skip` en lugar de `offset`, y `sortBy` + `order` para ordenar.

**3. Filtros combinados y page (Rick and Morty).** La respuesta es larga (20 personajes), así que la guardamos en un fichero.

`03-rickandmorty-filtros.sh`
```bash
curl -s -o pagina.json -w "HTTP %{http_code} | %{size_download} bytes\n" "https://rickandmortyapi.com/api/character/?status=dead&species=alien&page=2"
```

```text
HTTP 200 | 9485 bytes
```

Al principio de `pagina.json` está el objeto `info` con los metadatos:

```text
"info": {
  "count": 85,
  "pages": 5,
  "next": "https://rickandmortyapi.com/api/character/?page=3&status=dead&species=alien",
  "prev": "https://rickandmortyapi.com/api/character/?page=1&status=dead&species=alien"
}
Resultados en esta página: 20 | primero: Gobo | último: Stu
```

85 alienígenas muertos en 5 páginas de 20. Los enlaces `next` y `prev` **conservan los filtros**: el cliente solo tiene que seguirlos. Rick and Morty no deja elegir el tamaño de página.

**4. Enlaces en la cabecera `Link` (GitHub).** Las versiones publicadas de Spring Boot, de 2 en 2. Basta con las cabeceras (`-I`).

`04-github-link.sh`
```bash
curl -I "https://api.github.com/repos/spring-projects/spring-boot/releases?per_page=2"
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:12:46 GMT
content-type: application/json; charset=utf-8
link: <https://api.github.com/repositories/6296790/releases?per_page=2&page=2>; rel="next", <https://api.github.com/repositories/6296790/releases?per_page=2&page=180>; rel="last"
x-github-api-version-selected: 2022-11-28
x-ratelimit-limit: 60
x-ratelimit-remaining: 49
```

La cabecera `link` trae la siguiente página (`rel="next"`) y la última (`rel="last"`, la 180). El cuerpo lleva solo los datos.

**5. Las consultas de la biblioteca.**

`consultas-biblioteca.txt`
```text
CONSULTAS · API de la biblioteca (v1)

Parámetro   Ejemplo                       Para qué
----------  ----------------------------  -----------------------------------------
author      ?author=García Márquez        Filtro: libros cuyo autor contiene el texto
genre       ?genre=novela                 Filtro: género exacto
available   ?available=true               Filtro: solo los que tienen ejemplares
q           ?q=soledad                    Búsqueda libre en título y autor
sort        ?sort=title,asc               Ordenación: campo,dirección (asc|desc)
            ?sort=publishedYear,desc&sort=title,asc   (varios criterios)
page        ?page=0                       Número de página, empieza en 0
size        ?size=20                      Elementos por página (máximo 100)

Ejemplos
GET /api/v1/books?available=true&sort=title,asc
GET /api/v1/books?author=García Márquez&sort=publishedYear,desc&page=0&size=10
GET /api/v1/members/7/loans?status=ACTIVE&sort=dueDate,asc

Reglas
- Los filtros son parámetros con el nombre del campo. Varios filtros se combinan con Y.
- Sin page ni size: page=0 y size=20. Nunca se devuelve la colección entera.
- size mayor que 100 → se usa 100 (protege al servidor, OWASP API4).
- Un campo de sort que no existe → 400 con ProblemDetail.
- Sin resultados → 200 con content vacío, nunca 404.
- page, size y sort siguen la convención de Spring Data (Pageable, 10.05).
```

**6. Una página de libros** con el formato de Spring Data: la primera página (`number: 0`) de tamaño 2, de 5 libros en total.

`page-books.json`
```json
{
  "content": [
    { "isbn": "9788437604947", "title": "Cien años de soledad", "available": true },
    { "isbn": "9788497592208", "title": "Crónica de una muerte anunciada", "available": true }
  ],
  "page": {
    "size": 2,
    "number": 0,
    "totalElements": 5,
    "totalPages": 3
  }
}
```

```text
page-books.json: JSON válido
```

| Error frecuente | Causa | Solución |
|---|---|---|
| La página 1 de tu API devuelve la segunda página | En Spring Data las páginas empiezan en **0** | `page=0` es la primera; documéntalo en el contrato |
| Al pasar de página aparecen elementos repetidos | Paginar sin orden, o con un orden que no es único | Ordena siempre, y añade un desempate único (`sort=title,asc&sort=isbn,asc`) |
| En PowerShell, `The term 'limit=3' is not recognized…` o error con `&` | URL con `&` sin comillas | URL entre comillas dobles (módulo 02) |
| `{"error":"There is nothing here"}` con 404 | Rick and Morty: filtro sin resultados o página que no existe | Comprueba los filtros y que la página es menor o igual que `info.pages` |
| `HTTP/2 403` y `{"message":"API rate limit exceeded for <tu IP>. …"}` en GitHub | Sin autenticar, GitHub permite 60 peticiones por hora y por IP; en clase compartís IP | Espera a la hora indicada en `x-ratelimit-reset` o usa `-I` solo cuando haga falta |

### Práctica

1. **Rick and Morty.** Pide la **tercera página** de personajes humanos, vivos y de género femenino, guardándola en `pagina.json`. Abre el fichero: ¿cuántos personajes hay en total con esos filtros, cuántas páginas y cuántos en esta? ¿Qué vale `next`?
2. **DummyJSON.** Pide los portátiles (`/products/category/laptops`) **ordenados por precio de menor a mayor**, en páginas de 2, solo con `title` y `price`. Quiero la **tercera página**: calcula tú `skip`. ¿Cuántos productos llegan y por qué?
3. **PokeAPI.** Pide 5 Pokémon a partir del número 1350 (`offset=1349`). ¿Qué vale `next`? ¿Cuántos llegan?
4. **Consultas del gimnasio.** En `consultas-gimnasio.txt` escribe la petición (método y URI con parámetros) para:
   1. Las clases de spinning del 10/11/2026, ordenadas por hora de inicio.
   2. Las clases con plazas libres de un monitor, de 10 en 10, la segunda página.
   3. Las reservas canceladas de un socio, de la más reciente a la más antigua.
   4. Búsqueda libre "yoga" en el nombre y la descripción de las clases.
5. Escribe en `page-classes.json` la respuesta de la consulta 4.1 con el formato de Spring Data, para un día con dos clases de spinning.
6. Responde:
   1. En el apartado 2, ¿qué parámetros pondrías en la biblioteca para la misma consulta?
   2. ¿Qué ventaja tiene la cabecera `Link` de GitHub frente a los `next`/`previous` de PokeAPI? ¿Y qué inconveniente?
   3. Una app muestra un "scroll infinito" de las reservas de un socio mientras se crean reservas nuevas constantemente. ¿Qué tipo de paginación elegirías y por qué?

### Solución

**1. Rick and Morty.**

`01-rickandmorty.sh`
```bash
curl -s -o pagina.json -w "HTTP %{http_code} | %{size_download} bytes\n" "https://rickandmortyapi.com/api/character/?status=alive&species=human&gender=female&page=3"
```

```text
HTTP 200 | 8458 bytes
```

```text
"info": {
  "count": 56,
  "pages": 3,
  "next": null,
  "prev": "https://rickandmortyapi.com/api/character/?page=2&status=alive&species=human&gender=female"
}
Resultados en esta página: 16 | primero: Tammy Guetermann | último: Birdperson & Tammy's Child
```

56 personajes en 3 páginas. La tercera es la última: trae 16 (56 − 2 × 20) y `next` es `null`.

**2. DummyJSON.** La tercera página de tamaño 2 empieza en `skip = 2 × 2 = 4`.

`02-dummyjson-pagina3.sh`
```bash
curl "https://dummyjson.com/products/category/laptops?limit=2&skip=4&sortBy=price&order=asc&select=title,price"
```

```text
{"products":[{"id":78,"title":"Apple MacBook Pro 14 Inch Space Grey","price":1999.99}],"total":5,"skip":4,"limit":1}
```

Llega **un solo** portátil, el más caro: solo hay 5 (`total: 5`) y ya nos hemos saltado 4. DummyJSON lo indica bajando `limit` a 1. Es la última página.

**3. PokeAPI.**

`03-pokeapi-ultima.sh`
```bash
curl "https://pokeapi.co/api/v2/pokemon?offset=1349&limit=5"
```

```text
{"count":1351,"next":null,"previous":"https://pokeapi.co/api/v2/pokemon?offset=1344&limit=5","results":[{"name":"baxcalibur-mega","url":"https://pokeapi.co/api/v2/pokemon/10325/"},{"name":"meowstic-female-mega","url":"https://pokeapi.co/api/v2/pokemon/10326/"}]}
```

Pedimos 5, pero llegan 2: solo hay 1351 y empezamos en la posición 1349. `next` es `null` porque no hay más. Fíjate en que los identificadores (10325, 10326) no coinciden con la posición: el identificador es del recurso, la posición es de la lista.

**4. Consultas.**

`consultas-gimnasio.txt`
```text
CONSULTAS · API del gimnasio (v1) · soluciones

1. Clases de spinning del 10/11/2026, por hora de inicio:
   GET /api/v1/classes?type=spinning&date=2026-11-10&sort=startTime,asc

2. Clases con plazas libres de un monitor, 10 por página, segunda página:
   GET /api/v1/trainers/4/classes?hasFreeSpots=true&page=1&size=10

3. Reservas canceladas de un socio, de la más reciente a la más antigua:
   GET /api/v1/members/7/bookings?status=CANCELLED&sort=createdAt,desc

4. Búsqueda libre "yoga" en nombre y descripción:
   GET /api/v1/classes?q=yoga

Decisiones
- Fechas en ISO 8601 (2026-11-10), como en el JSON (02.04).
- page empieza en 0 (convención de Spring Data): la segunda página es page=1.
- Filtros con el nombre del campo; hasFreeSpots es un campo calculado, pero
  para el cliente es igual que cualquier otro filtro.
```

**5. Respuesta paginada.**

`page-classes.json`
```json
{
  "content": [
    { "id": 41, "type": "spinning", "startTime": "2026-11-10T09:00:00", "freeSpots": 3 },
    { "id": 44, "type": "spinning", "startTime": "2026-11-10T18:00:00", "freeSpots": 0 }
  ],
  "page": {
    "size": 2,
    "number": 0,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

```text
page-classes.json: JSON válido
```

**6. Preguntas.**
1. `GET /api/v1/books?genre=laptops&sort=price,asc&page=2&size=2`, con `genre` o el filtro equivalente de la categoría. La página 2 (empezando en 0) es la tercera.
2. Ventaja: el cuerpo queda solo con los datos y los enlaces siguen un estándar (RFC 8288) que entienden muchas librerías. Inconveniente: hay que leer y analizar una cabecera, que es menos cómodo que un campo JSON, y no se ve al abrir la respuesta en el navegador.
3. **Cursor.** Con offset o page, cada reserva nueva desplaza la lista y el usuario vería elementos repetidos al cargar la siguiente página. El cursor ("dame las 20 siguientes a la reserva X") no se ve afectado por las inserciones, y en un scroll infinito no hace falta saltar a una página concreta.

---

## 03.05 · Versionado y evolución de un API

### Resumen
- Un API publicado es un **contrato**: hay clientes que dependen de él y que no se actualizan a la vez que tu servidor.
- **Cambios compatibles** (no rompen): añadir campos a la respuesta, endpoints nuevos, parámetros opcionales. **Cambios incompatibles** (*breaking changes*): quitar o renombrar campos, cambiar tipos, hacer obligatorio algo opcional, cambiar códigos o URIs.
- Los cambios compatibles se publican en la versión actual; los incompatibles, en una **versión nueva** que convive con la anterior durante un tiempo.
- Formas de indicar la versión: en la **URI** (`/api/v1`, la del curso), en una **cabecera** o en el **tipo de contenido**.
- La retirada se avisa con las cabeceras **`Deprecation`** (RFC 9745) y **`Sunset`** (RFC 8594), y con `Link` hacia la versión sucesora.

### Teoría

**Por qué hace falta versionar.** Tu API de la biblioteca la usan una web, una app móvil (que cada usuario actualiza cuando quiere) y quizá la aplicación de otro centro. Si mañana renombras `title` a `name`, las tres dejan de mostrar los títulos. No puedes obligar a todos los clientes a actualizarse a la vez que tu servidor. Por eso, cuando un cambio rompe, se publica una **nueva versión** y la anterior sigue funcionando un tiempo.

**¿Qué rompe y qué no?** Piensa como un cliente que ya funciona y no se va a tocar.

| Tipo de cambio | ¿Rompe? |
|---|---|
| Añadir un campo a la respuesta | No (los clientes ignoran lo que no conocen) |
| Añadir un endpoint | No |
| Añadir un parámetro **opcional** | No |
| Aceptar un valor más en una petición | No |
| Añadir un valor a un enum de la respuesta | Puede: el cliente que hace `switch` sobre los valores conocidos no sabe qué hacer |
| Quitar o renombrar un campo | Sí |
| Cambiar el tipo o el formato de un campo (texto → objeto, fecha → número) | Sí |
| Hacer obligatorio un campo de la petición | Sí |
| Cambiar una URI o un método | Sí |
| Cambiar el código de estado de una respuesta | Sí |
| Cambiar las reglas de validación para ser más estricto | Sí |

Para que los cambios compatibles lo sean de verdad, los clientes deben ser **tolerantes**: ignorar los campos que no conocen y no fallar con valores nuevos. Es la idea del *principio de robustez*: sé estricto en lo que envías y tolerante en lo que aceptas.

**Dónde va la versión.**

| Estrategia | Ejemplo | A favor | En contra |
|---|---|---|---|
| En la URI | `/api/v1/books` | Visible, fácil de probar con curl o el navegador, fácil de enrutar | "Ensucia" la URI: el libro es el mismo en v1 y en v2 |
| Cabecera propia | `X-GitHub-Api-Version: 2026-03-10` | URIs limpias | Invisible: hay que acordarse de enviarla |
| Tipo de contenido | `Accept: application/vnd.biblioteca.v2+json` | Muy fiel a REST (la versión es de la representación) | Complejo para el cliente y las herramientas |
| Parámetro de consulta | `/books?version=2` | Sencillo | Se mezcla con los filtros; poco usado |

En el curso usamos **la URI** (ficha del curso), como PokeAPI (`/api/v2`), Open-Meteo (`/v1/forecast`) o Frankfurter (`/v1` y `/v2`). GitHub usa una cabecera con una fecha como versión; su lista de versiones vigentes se consulta en `https://api.github.com/versions`.

Spring Framework 7 (el de Spring Boot 4) incluye soporte de versionado: se puede indicar la versión en los mappings (`@GetMapping(path = "/{isbn}", version = "2")`) y elegir de dónde se lee (ruta, cabecera, parámetro o tipo de contenido), por ejemplo con la propiedad `spring.mvc.apiversion.use.header`. No lo necesitamos para el proyecto, porque con la versión en la ruta basta un `@RequestMapping("/api/v2/books")`, pero conviene saber que existe.

**Qué es una versión.** Solo se sube la versión **mayor** con los cambios incompatibles. Es la idea del versionado semántico (`MAYOR.MENOR.PARCHE`) aplicada al API: `v1` puede recibir durante años campos y endpoints nuevos sin cambiar de nombre. No crees `v2` por cada despliegue.

**El ciclo de vida de una versión.**
1. **Publicación** de v2. v1 sigue funcionando igual.
2. **Deprecación** de v1: sigue funcionando, pero se avisa de que va a desaparecer. Las respuestas llevan:
   - `Deprecation: @1798761600`: desde cuándo está deprecada, como número de segundos desde 1970 precedido de `@` (RFC 9745, de 2025).
   - `Sunset: Wed, 30 Jun 2027 23:59:59 GMT`: cuándo dejará de funcionar, en formato de fecha HTTP (RFC 8594).
   - `Link: </api/v2/books>; rel="successor-version"`: dónde está la versión nueva.
3. **Retirada** de v1 en la fecha `Sunset`: responde **410 Gone** ("existió, pero ya no está") o deja de existir.

Mientras conviven, cuantas menos versiones, mejor: cada versión viva es código que mantener y probar.

### Código de ejemplo

**1 y 2. Dos versiones conviviendo (Frankfurter).** En el módulo 02 usaste la v2 de Frankfurter. La v1 sigue funcionando, pero está deprecada.

`01-frankfurter-v1.sh`
```bash
curl -i "https://api.frankfurter.dev/v1/latest?symbols=USD"
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:12:51 GMT
content-type: application/json; charset=utf-8
deprecation: @1779103800
link: <https://api.frankfurter.dev/v2/rates>; rel="successor-version"

{"amount":1.0,"base":"EUR","date":"2026-10-30","rates":{"USD":1.1172}}
```

`02-frankfurter-v2.sh`
```bash
curl -i "https://api.frankfurter.dev/v2/rates?base=EUR&quotes=USD"
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:12:52 GMT
content-type: application/json; charset=utf-8

[{"date":"2026-11-02","base":"EUR","quote":"USD","rate":1.118}]
```

Qué observar:
- La v1 responde con `deprecation: @1779103800` (el 18 de mayo de 2026, 11:30 UTC) y un `link` con `rel="successor-version"` hacia la v2. No envía `Sunset`: todavía no ha fijado la fecha de retirada.
- La forma de la respuesta ha cambiado: v1 devuelve un objeto con `rates` (`{"USD": 1.1172}`); v2, una **lista** de objetos con `quote` y `rate`. Es un cambio incompatible: un cliente de v1 no sabría leer v2. Por eso es una versión nueva.
- La fecha de los datos de v1 (`2026-10-30`) es anterior a la de v2 (`2026-11-02`): una versión deprecada puede dejar de recibir mejoras.

**3. Una versión retirada (PokeAPI).** PokeAPI tuvo una v1 que retiró hace años. Ahora sus URLs redirigen a la v2.

`03-pokeapi-v1.sh`
```bash
curl -i https://pokeapi.co/api/v1/pokemon/25
```

```text
HTTP/2 301 
date: Mon, 02 Nov 2026 09:12:56 GMT
content-type: text/plain; charset=utf-8
location: /api/v2/pokemon/25

Redirecting to /api/v2/pokemon/25
```

Redirigir es una opción amable, pero solo funciona si la respuesta nueva es compatible con la antigua. Si no lo es, lo honesto es **410 Gone** con un Problem Details que explique a dónde ir.

**4. La política de versiones de la biblioteca.**

`cambios-biblioteca.txt`
```text
EVOLUCIÓN DEL API DE LA BIBLIOTECA · ¿rompe a los clientes?

Cambio                                             ¿Rompe?  Qué hacer
-------------------------------------------------  -------  ------------------------
Añadir "language" a la respuesta de un libro       No       Publicar en v1
Añadir GET /api/v1/books/{isbn}/loans              No       Publicar en v1
Añadir el filtro opcional ?language=es             No       Publicar en v1
Añadir el valor "LOST" al enum status              Puede    Avisar antes; los clientes
                                                            deben tolerar valores nuevos
Renombrar "title" a "name"                         Sí       v2
Cambiar "author" de texto a objeto {id, name}      Sí       v2
Hacer obligatorio un campo opcional al crear       Sí       v2
Quitar el campo "copies"                           Sí       v2
Cambiar el identificador de ISBN a id numérico     Sí       v2
Cambiar 200 por 204 en DELETE                      Sí       v2

Plan de convivencia v1 → v2
1. Se publica /api/v2/books con los cambios. /api/v1 sigue funcionando igual.
2. Las respuestas de v1 llevan cabeceras de aviso (RFC 9745 y RFC 8594):
     Deprecation: @1798761600                  (desde el 01/01/2027)
     Sunset: Wed, 30 Jun 2027 23:59:59 GMT     (fecha en que se retira)
     Link: </api/v2/books>; rel="successor-version"
3. Se avisa a los clientes y se mide quién sigue usando v1.
4. Llegada la fecha Sunset, v1 responde 410 Gone (o se retira).
```

| Error frecuente | Causa | Solución |
|---|---|---|
| Al renombrar un campo, la app móvil deja de mostrar datos | Cambio incompatible publicado en la misma versión | Mantén el nombre en v1 y renómbralo solo en v2 |
| Un cliente falla con `Unrecognized field "language"` al añadir un campo | El cliente no es tolerante: rechaza los campos que no conoce | Configura el cliente para ignorarlos (Jackson lo hace por defecto en Spring Boot, 06.04) |
| `v1`, `v2`, `v3`… en pocas semanas | Subir de versión por cualquier cambio | Solo los cambios incompatibles requieren versión nueva |
| `Deprecation: Mon, 18 May 2026 11:30:00 GMT` | Formato antiguo del borrador; el RFC 9745 usa `@` y segundos | `Deprecation: @1779103800` |

### Práctica

1. **GitHub.** Consulta con curl qué versiones de su API admite GitHub (`https://api.github.com/versions`). ¿Qué formato usan como identificador de versión? ¿Cuál es la más reciente?
2. **Frankfurter.** Haz una petición HEAD a `https://api.frankfurter.dev/v1/currencies`. ¿Qué cabeceras indican que está deprecada y a dónde debes migrar? Convierte el valor de `Deprecation` a fecha (con `date -u -d @1779103800` en Linux o Git Bash, o con cualquier conversor de *timestamp* Unix en la web).
3. **Clasifica** estos cambios del API del gimnasio como compatibles o incompatibles, explicando por qué, en `clasificacion-gimnasio.txt`:
   1. Añadir `"room"` a la respuesta de una clase.
   2. Cambiar `"startTime"` de `"2026-11-10T18:00:00"` a `1794337200` (segundos desde 1970).
   3. Añadir el parámetro `?level=beginner` a `GET /api/v1/classes`.
   4. Exigir `"phone"` al dar de alta un socio.
   5. Pasar de `PATCH /api/v1/bookings/{id}` a `POST /api/v1/bookings/{id}/cancellation` para cancelar.
   6. Devolver 409 en lugar de 400 al reservar sin plazas.
   7. Añadir el estado `"WAITLISTED"` a las reservas.
4. En el mismo fichero, escribe las tres cabeceras que añadirías a las respuestas de la v1 de clases si se depreca el 01/02/2027 y se retira el 31/07/2027 a las 23:59:59 UTC. Para el valor de `Deprecation` usa `date -u -d 2027-02-01 +%s` o un conversor.
5. Responde:
   1. ¿Por qué el cambio 6 rompe, si 409 es "más correcto" que 400?
   2. ¿Qué ventaja tiene para el cliente que `Sunset` sea una fecha concreta y no "pronto"?

### Solución

**1. Versiones de GitHub.**

`01-github-versiones.sh`
```bash
curl https://api.github.com/versions
```

```text
[
  "2026-03-10",
  "2022-11-28"
]
```

Usa **fechas** como versión (`AAAA-MM-DD`): la más reciente es `2026-03-10`. Si no se envía la cabecera `X-GitHub-Api-Version`, GitHub usa `2022-11-28`, la más antigua de las vigentes, para no romper a los clientes que nunca la enviaron.

**2. Frankfurter.**

`02-frankfurter-v1-head.sh`
```bash
curl -I https://api.frankfurter.dev/v1/currencies
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:12:59 GMT
content-type: application/json; charset=utf-8
deprecation: @1779103800
link: <https://api.frankfurter.dev/v2/currencies>; rel="successor-version"
```

`deprecation` dice que está deprecada desde `@1779103800`, que es el **18 de mayo de 2026 a las 11:30 UTC**; `link` con `rel="successor-version"` indica el sustituto: `https://api.frankfurter.dev/v2/currencies`.

**3 y 4. Clasificación y cabeceras.**

`clasificacion-gimnasio.txt`
```text
EVOLUCIÓN DEL API DEL GIMNASIO · soluciones

1. Añadir "room" a la respuesta de una clase
   No rompe: un cliente que no conoce el campo lo ignora.
2. Cambiar "startTime" de "2026-11-10T18:00:00" a 1794337200
   Rompe: cambia el tipo del campo y el cliente no sabe leerlo.
3. Añadir ?level=beginner a GET /api/v1/classes
   No rompe: es opcional; sin él, la respuesta es la misma.
4. Exigir "phone" al dar de alta un socio
   Rompe: las peticiones de siempre empiezan a dar 400.
5. Pasar de PATCH /bookings/{id} a POST /bookings/{id}/cancellation
   Rompe: la petición antigua deja de funcionar (405 o 404).
6. Devolver 409 en lugar de 400 al reservar sin plazas
   Rompe: el cliente que comprobaba el 400 deja de detectarlo.
7. Añadir el estado "WAITLISTED" a las reservas
   Puede romper: a los clientes que solo esperan los valores antiguos.

Cabeceras de las respuestas de v1 (deprecada el 01/02/2027,
retirada el 31/07/2027 a las 23:59:59 UTC):
  Deprecation: @1801440000
  Sunset: Sat, 31 Jul 2027 23:59:59 GMT
  Link: </api/v2/classes>; rel="successor-version"
```

`@1801440000` es el 01/02/2027 a las 00:00 UTC; el 31/07/2027 es sábado.

**5. Preguntas.**
1. Porque los clientes no leen la documentación, leen las **respuestas**: una app que mostraba "Clase completa" cuando recibía 400 deja de hacerlo con 409 y muestra un error genérico. Un cambio de código es un cambio del contrato aunque el nuevo sea mejor. Se hace en v2, o se decide bien desde el principio (por eso diseñamos antes de programar).
2. Que puede planificar: sabe cuánto tiempo tiene para migrar, puede programar avisos y no se lleva la sorpresa de que su aplicación deje de funcionar un día cualquiera.

---

## 03.06 · Seguridad en APIs: conceptos (HTTPS, credenciales, tokens)

### Resumen
- **HTTPS siempre**: cifra todo lo que viaja, incluidas las credenciales y los tokens. Con HTTP, cualquiera en la misma red puede leerlos.
- **Autenticación** (¿quién eres?) y **autorización** (¿qué puedes hacer?) son cosas distintas: fallar la primera es 401; la segunda, 403.
- Formas de enviar la identidad: **Basic** (usuario y contraseña en cada petición), **API key** (una clave fija) y **token Bearer** (un token temporal obtenido con un login). Siempre en la cabecera `Authorization` (o una cabecera propia), **nunca en la URL**.
- Un **JWT** tiene tres partes en Base64URL: cabecera, datos (*claims*) y firma. Cualquiera puede **leerlo**; solo quien tiene la clave puede **crearlo o modificarlo** sin que se note.
- El **OWASP API Security Top 10** recoge los fallos más frecuentes. El primero, **BOLA**: no comprobar que el recurso pedido es del usuario.

### Teoría

**HTTPS.** HTTP viaja en texto plano: en una wifi pública, cualquiera puede leer las peticiones, contraseñas y tokens incluidos. HTTPS es HTTP dentro de una conexión **TLS**, que cifra el contenido, garantiza que no se modifica por el camino y demuestra, con un certificado, que el servidor es quien dice ser. En un API:
- Solo se sirve por HTTPS. Las peticiones por HTTP se redirigen (301) o se rechazan.
- La cabecera `Strict-Transport-Security` (HSTS) indica al navegador que use siempre HTTPS con ese dominio.
- En desarrollo (`localhost`) se usa HTTP; en cuanto se despliega, HTTPS (módulo 13).

**Autenticación y autorización.**

| | Autenticación | Autorización |
|---|---|---|
| Pregunta | ¿Quién eres? | ¿Puedes hacer esto? |
| Se comprueba con | Credenciales o token | Roles, propiedad del recurso |
| Si falla | **401** + `WWW-Authenticate` | **403** |
| Ejemplo | Token caducado | Un socio intenta borrar un libro |

**Formas de enviar la identidad.**

| Mecanismo | Cómo viaja | Para qué | Riesgos |
|---|---|---|---|
| **Basic** (RFC 7617) | `Authorization: Basic base64(usuario:contraseña)` en **cada** petición | Herramientas internas, pruebas | La contraseña viaja siempre; Base64 **no es cifrado** |
| **API key** | Una cabecera propia (`X-API-Key: …`) | Identificar a la **aplicación** cliente (Open-Meteo comercial, Google Maps) | Es fija: si se filtra, sirve hasta que se revoca |
| **Token Bearer** | `Authorization: Bearer <token>` | Usuarios: se hace login una vez y se usa el token | Quien tenga el token es "tú" hasta que caduque |
| **Cookie de sesión** | `Cookie: …` (el navegador la envía sola) | Webs tradicionales | Exige estado en el servidor; ataques CSRF |

**Nunca en la URL.** `GET /api/v1/books?token=eyJ…` funciona, pero la URL queda en el historial del navegador, en los logs del servidor y de los proxies, y en la cabecera `Referer` si la página carga recursos de otro sitio. Las credenciales van **siempre en cabeceras**, y sobre HTTPS.

**El flujo con tokens.** Es el que usarás en el módulo 11 (y el que viste con DummyJSON en 02.05):
1. `POST /api/v1/auth/login` con usuario y contraseña → el servidor devuelve un **token de acceso** (de vida corta: minutos) y, a veces, un **token de refresco** (de vida larga).
2. Cada petición lleva `Authorization: Bearer <token de acceso>`. El servidor comprueba el token **sin guardar sesión** (sin estado, 03.01).
3. Cuando el token de acceso caduca, el cliente pide otro con el de refresco (`POST /auth/refresh`) sin volver a pedir la contraseña.

**JWT por dentro.** Un JWT (*JSON Web Token*, RFC 7519) es un texto con tres partes separadas por puntos: `cabecera.payload.firma`.
- **Cabecera**: el algoritmo de firma. `{"alg":"HS256","typ":"JWT"}`.
- **Payload**: los datos (*claims*): quién es (`sub`, `id`, `username`), cuándo se emitió (`iat`) y cuándo caduca (`exp`), en segundos desde 1970.
- **Firma**: calculada con la cabecera, el payload y una **clave secreta** del servidor.

Las dos primeras partes están en **Base64URL**, que es una codificación, no un cifrado: cualquiera las puede leer (lo harás en el ejemplo). La firma impide **modificarlas**: si alguien cambia `"role":"user"` por `"role":"admin"`, la firma deja de coincidir y el servidor rechaza el token. Consecuencias:
- No metas en un JWT nada secreto (contraseñas, datos personales innecesarios).
- El servidor siempre comprueba la firma y la caducidad. Spring Security lo hace por ti (11.03).
- Un token robado es válido hasta que caduca: por eso duran poco.

**OWASP API Security Top 10 (edición 2023).** OWASP es una fundación que publica las listas de referencia de riesgos de seguridad. La de APIs es esta:

| Riesgo | En la biblioteca | Dónde lo resolvemos |
|---|---|---|
| API1 · Broken Object Level Authorization (BOLA) | El socio 7 pide `/members/8/loans` y el API se los da | Comprobar la propiedad (11.04) |
| API2 · Broken Authentication | Tokens que no caducan, login sin límite de intentos | 11.03 |
| API3 · Broken Object Property Level Authorization | La respuesta del socio incluye su contraseña; o un socio envía `"role": "LIBRARIAN"` al darse de alta y se lo aplican | DTOs separados (06.04) |
| API4 · Unrestricted Resource Consumption | `?size=1000000` | Límites de paginación (03.04) |
| API5 · Broken Function Level Authorization | Un socio llama a `DELETE /books/{isbn}` y funciona | Roles (11.04) |
| API6 · Unrestricted Access to Sensitive Business Flows | Un script reserva todos los ejemplares de un libro nuevo | Límites por usuario |
| API7 · Server Side Request Forgery | El API descarga una portada desde una URL que envía el cliente | Validar URLs (12) |
| API8 · Security Misconfiguration | Trazas de error al cliente, CORS abierto a todos | 07.05, 11.05 |
| API9 · Improper Inventory Management | La v1 olvidada sigue funcionando sin las correcciones de seguridad | Política de versiones (03.05) |
| API10 · Unsafe Consumption of APIs | Confiar ciegamente en lo que devuelve un API externa | 12.03 |

### Código de ejemplo

**1. HTTP se redirige a HTTPS.**

`01-http-a-https.sh`
```bash
curl -I http://pokeapi.co/api/v2/pokemon/25
```

```text
HTTP/1.1 301 Moved Permanently
Date: Mon, 02 Nov 2026 09:13:00 GMT
Content-Type: text/html; charset=UTF-8
Location: https://pokeapi.co/api/v2/pokemon/25
```

La petición por HTTP no llega al API: el servidor responde 301 y la URL `https://` equivalente. Fíjate en que la petición original **ya ha viajado sin cifrar**: si llevara un token, se habría expuesto. Por eso los clientes deben usar `https://` desde el principio.

**2. Autenticación Basic.** Postman Echo es un servicio de pruebas con un endpoint protegido con Basic (usuario `postman`, contraseña `password`). Con `-v` vemos la cabecera que envía curl.

`02-basic-auth.sh`
```bash
curl -v -u postman:password https://postman-echo.com/basic-auth
```

```text
> GET /basic-auth HTTP/2
> Host: postman-echo.com
> authorization: Basic cG9zdG1hbjpwYXNzd29yZA==
> user-agent: curl/7.81.0
> accept: */*
> 
< HTTP/2 200 
< content-type: application/json; charset=utf-8
< 
{"authenticated":true}
```

`-u postman:password` se convierte en `authorization: Basic cG9zdG1hbjpwYXNzd29yZA==`. Ese texto es `postman:password` en Base64: cualquiera lo descodifica en un segundo. Solo HTTPS lo protege.

**3. Credenciales incorrectas.**

`03-basic-auth-mal.sh`
```bash
curl -i -u postman:otra https://postman-echo.com/basic-auth
```

```text
HTTP/2 401 
date: Mon, 02 Nov 2026 09:13:03 GMT
www-authenticate: Basic realm="Users"

Unauthorized
```

401 con la cabecera `www-authenticate: Basic realm="Users"`, que dice al cliente qué mecanismo usar. Es la respuesta de manual.

**4. Tokens JWT con Bruno.** Ampliamos la colección de DummyJSON de 02.05. El test del login **descodifica el payload** del token, sin conocer la clave, y comprueba su caducidad.

`opencollection.yml`
```yaml
opencollection: 1.0.0

info:
  name: DummyJSON JWT
```

`environments/dev.yml`
```yaml
name: dev
variables:
  - name: baseUrl
    value: https://dummyjson.com
  - name: username
    value: emilys
  - name: password
    value: emilyspass
```

`01-login.yml`
```yaml
info:
  name: 01 Login
  type: http
  seq: 1

http:
  method: POST
  url: "{{baseUrl}}/auth/login"
  body:
    type: json
    data: |-
      {
        "username": "{{username}}",
        "password": "{{password}}",
        "expiresInMins": 30
      }

runtime:
  scripts:
    - type: tests
      code: |-
        // El payload es la segunda parte del token, en Base64URL: se puede leer sin la clave
        const payload = JSON.parse(atob(res.body.accessToken.split(".")[1]
          .replace(/-/g, "+").replace(/_/g, "/")));
        console.log("Payload del token:", JSON.stringify(payload));
        test("el token lleva el usuario y caduca en 30 minutos", function() {
          expect(payload.username).to.equal(bru.getEnvVar("username"));
          expect(payload.exp - payload.iat).to.equal(1800);
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.accessToken
        method: jsonq
      variable:
        name: accessToken
        scope: runtime
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.refreshToken
        method: jsonq
      variable:
        name: refreshToken
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`02-me.yml`
```yaml
info:
  name: 02 Quién soy (con token)
  type: http
  seq: 2

http:
  method: GET
  url: "{{baseUrl}}/auth/me"
  auth:
    type: bearer
    token: "{{accessToken}}"

runtime:
  scripts:
    - type: tests
      code: |-
        test("devuelve el usuario del token", function() {
          expect(res.body.username).to.equal(bru.getEnvVar("username"));
        });
        test("OWASP API3: la respuesta expone la contraseña y la tarjeta", function() {
          expect(res.body).to.have.property("password");
          expect(res.body.bank).to.have.property("cardNumber");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`03-me-tampered.yml`. El token con una `x` añadida al final: la firma ya no coincide.

```yaml
info:
  name: 03 Token manipulado
  type: http
  seq: 3

http:
  method: GET
  url: "{{baseUrl}}/auth/me"
  auth:
    type: bearer
    token: "{{accessToken}}x"

runtime:
  scripts:
    - type: tests
      code: |-
        test("rechaza el token (DummyJSON responde 500; lo correcto sería 401)", function() {
          expect(res.body.message).to.equal("invalid signature");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "500"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`04-refresh.yml`
```yaml
info:
  name: 04 Renovar el token
  type: http
  seq: 4

http:
  method: POST
  url: "{{baseUrl}}/auth/refresh"
  body:
    type: json
    data: |-
      {
        "refreshToken": "{{refreshToken}}",
        "expiresInMins": 30
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("devuelve un token de acceso nuevo", function() {
          expect(res.body.accessToken).to.be.a("string").and.not.be.empty;
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.accessToken
        method: jsonq
      variable:
        name: accessToken
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`05-me-new-token.yml`
```yaml
info:
  name: 05 Quién soy (token renovado)
  type: http
  seq: 5

http:
  method: GET
  url: "{{baseUrl}}/auth/me"
  auth:
    type: bearer
    token: "{{accessToken}}"

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

El test sustituye `-` y `_` por `+` y `/` antes de llamar a `atob` porque el token está en Base64**URL**, una variante de Base64 que se puede poner en una URL sin escapar.

✅ **Si ves esto, vas bien** (`bru run --env dev`; en la aplicación, lo que se escribe con `console.log` aparece en la consola de Bruno):
```text
01-login (200 OK) - 196 ms
Payload del token: {"id":1,"username":"emilys","email":"emily.johnson@x.dummyjson.com","firstName":"Emily","lastName":"Johnson","gender":"female","image":"https://dummyjson.com/icon/emilys/128","iat":1793610786,"exp":1793612586}
Tests
   ✓ el token lleva el usuario y caduca en 30 minutos
Assertions
   ✓ res.status: eq 200
02-me (200 OK) - 238 ms
Tests
   ✓ devuelve el usuario del token
   ✓ OWASP API3: la respuesta expone la contraseña y la tarjeta
Assertions
   ✓ res.status: eq 200
03-me-tampered (500 Internal Server Error) - 198 ms
Tests
   ✓ rechaza el token (DummyJSON responde 500; lo correcto sería 401)
Assertions
   ✓ res.status: eq 500
04-refresh (200 OK) - 172 ms
Tests
   ✓ devuelve un token de acceso nuevo
Assertions
   ✓ res.status: eq 200
05-me-new-token (200 OK) - 152 ms
Assertions
   ✓ res.status: eq 200
📊 Execution Summary
┌───────────────┬──────────────┐
│ Metric        │    Result    │
├───────────────┼──────────────┤
│ Status        │    ✓ PASS    │
├───────────────┼──────────────┤
│ Requests      │ 5 (5 Passed) │
├───────────────┼──────────────┤
│ Tests         │     5/5      │
├───────────────┼──────────────┤
│ Assertions    │     5/5      │
├───────────────┼──────────────┤
│ Duration (ms) │     2123     │
└───────────────┴──────────────┘
```

Qué observar:
- El **payload** se lee sin la clave: id, usuario, email, nombre, `iat` y `exp`. `exp − iat = 1800` segundos, los 30 minutos que pedimos.
- **API3 en directo**: `/auth/me` devuelve, entre otros datos, la **contraseña** del usuario y el número de su tarjeta. Es un API de pruebas con datos inventados, pero en un API real sería un fallo grave. Tu API nunca devolverá la entidad tal cual: usarás DTOs (06.04).
- El token manipulado se rechaza (la firma no coincide), aunque con un 500 en lugar del 401 correcto, como viste en 03.03.
- Con el token de refresco se obtiene un token de acceso nuevo sin enviar la contraseña.

También puedes pegar un token en el depurador de https://jwt.io, que lo descodifica en tu navegador. Hazlo solo con tokens de prueba.

**5. Quién puede hacer qué en la biblioteca.**

`permisos-biblioteca.txt`
```text
QUIÉN PUEDE HACER QUÉ · API de la biblioteca (se implementa en el módulo 11)

Endpoint                          Anónimo   Socio (MEMBER)        Bibliotecario (LIBRARIAN)
--------------------------------  --------  --------------------  -------------------------
GET    /api/v1/books              Sí        Sí                    Sí
GET    /api/v1/books/{isbn}       Sí        Sí                    Sí
POST   /api/v1/books              401       403                   Sí
PUT    /api/v1/books/{isbn}       401       403                   Sí
DELETE /api/v1/books/{isbn}       401       403                   Sí
GET    /api/v1/members/{id}       401       Solo el suyo (*)      Sí
GET    /api/v1/members/{id}/loans 401       Solo los suyos (*)    Sí
POST   /api/v1/loans              401       403                   Sí
PATCH  /api/v1/loans/{id}         401       403                   Sí

(*) Si el socio 7 pide /api/v1/members/8 → 403 (o 404, para no revelar que existe).
    Comprobar que el recurso es del usuario del token es la defensa contra
    OWASP API1 (Broken Object Level Authorization), el fallo más común.

Reglas que aplicaremos
- Solo HTTPS. Credenciales y tokens siempre en la cabecera Authorization,
  nunca en la URL (las URLs se guardan en logs e historiales).
- Login: POST /api/v1/auth/login → JWT con caducidad corta (p. ej. 30 min).
- Sin estado en el servidor: cada petición lleva su token (03.01).
- Respuestas sin datos sensibles: un socio nunca lleva su contraseña (DTO, 06.04).
- 401 sin token o token no válido; 403 con token válido pero sin permiso.
- Límites: size máximo en las listas (03.04) y límite de peticiones (429).
```

| Error frecuente | Causa | Solución |
|---|---|---|
| `HTTP/1.1 301 Moved Permanently` y respuesta vacía | Has escrito `http://` y el servidor redirige a HTTPS | Escribe `https://` (o añade `-L`, pero la primera petición ya ha viajado sin cifrar) |
| `401` y `www-authenticate: Basic realm="Users"` | Usuario o contraseña incorrectos | Revisa las credenciales; en curl, `-u usuario:contraseña` |
| `{"message":"Access Token is required"}` con 401 | La petición no lleva el token | Pestaña **Auth** de la petición, tipo **Bearer Token**, con `{{accessToken}}` |
| `{"message":"Invalid/Expired Token!"}` con 401 | El token ha caducado o no es válido | Repite el login o usa el token de refresco |
| Un token pegado en un chat, un issue o un repositorio | Tratar el token como un texto cualquiera | Es como una contraseña temporal: no lo compartas; si se filtra, cámbialo |

### Práctica

1. **HTTPS.** Haz una petición HEAD por **HTTP** a `http://dummyjson.com/products/1`. ¿Qué responde y a dónde te manda?
2. **Basic a mano.** Autentícate en `https://postman-echo.com/basic-auth`:
   1. con `-u postman:password`;
   2. escribiendo tú la cabecera `Authorization` completa. Para calcular el Base64 de `postman:password` usa un codificador web o, en Git Bash, `printf 'postman:password' | base64`.
3. **Colección `JWT michaelw`** con DummyJSON, para el usuario `michaelw` (contraseña `michaelwpass`):
   1. Login con un token que dure **5 minutos**. En un test, descodifica el payload y comprueba que el usuario es `michaelw`, su `id` es 2 y que `exp − iat` son 300 segundos. Muestra en la consola el payload y la fecha de caducidad en formato ISO.
   2. `GET /auth/me` con el token. Comprueba el `id` y el `role` que devuelve.
   3. Login con una contraseña equivocada. Comprueba el código y el mensaje.
   4. Renueva el token y repite `GET /auth/me` con el nuevo.
4. **Permisos del gimnasio.** Escribe `permisos-gimnasio.txt` con la matriz de quién puede hacer qué (anónimo, socio, monitor y recepción) para los endpoints principales, y las comprobaciones de propiedad necesarias contra BOLA.
5. Responde:
   1. Si un compañero intercepta tu token de DummyJSON en la wifi del centro, ¿qué puede hacer y durante cuánto tiempo? ¿Qué lo habría evitado?
   2. ¿Por qué Basic sobre HTTPS es aceptable para una herramienta interna, pero no para la app de los socios?
   3. En el apartado 3.2, `role` vale `admin`. ¿Debería decidir el **cliente** qué botones muestra según ese campo? ¿Y el **servidor** qué permite?

> **Pista:** `new Date(payload.exp * 1000).toISOString()` convierte la fecha de caducidad. `exp` está en segundos y `Date` trabaja en milisegundos.

### Solución

**1. HTTPS.**

`01-http-a-https.sh`
```bash
curl -I http://dummyjson.com/products/1
```

```text
HTTP/1.1 301 Moved Permanently
Date: Mon, 02 Nov 2026 09:13:08 GMT
Content-Type: text/html; charset=UTF-8
Location: https://dummyjson.com/products/1
```

301 hacia `https://dummyjson.com/products/1`.

**2. Basic a mano.** `printf 'postman:password' | base64` da `cG9zdG1hbjpwYXNzd29yZA==`.

`02-basic-u.sh`
```bash
curl -i -u postman:password https://postman-echo.com/basic-auth
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:13:09 GMT
content-type: application/json; charset=utf-8

{"authenticated":true}
```

`03-basic-cabecera.sh`
```bash
curl -i -H "Authorization: Basic cG9zdG1hbjpwYXNzd29yZA==" https://postman-echo.com/basic-auth
```

```text
HTTP/2 200 
date: Mon, 02 Nov 2026 09:13:11 GMT
content-type: application/json; charset=utf-8

{"authenticated":true}
```

Mismo resultado: `-u` solo es un atajo para escribir esa cabecera.

**3. Colección `JWT michaelw`.**

`opencollection.yml`
```yaml
opencollection: 1.0.0

info:
  name: JWT michaelw
```

`environments/dev.yml`
```yaml
name: dev
variables:
  - name: baseUrl
    value: https://dummyjson.com
  - name: username
    value: michaelw
  - name: password
    value: michaelwpass
```

`01-login.yml`
```yaml
info:
  name: 01 Login (5 minutos)
  type: http
  seq: 1

http:
  method: POST
  url: "{{baseUrl}}/auth/login"
  body:
    type: json
    data: |-
      {
        "username": "{{username}}",
        "password": "{{password}}",
        "expiresInMins": 5
      }

runtime:
  scripts:
    - type: tests
      code: |-
        const payload = JSON.parse(atob(res.body.accessToken.split(".")[1]
          .replace(/-/g, "+").replace(/_/g, "/")));
        console.log("Payload:", JSON.stringify(payload));
        console.log("Caduca:", new Date(payload.exp * 1000).toISOString());
        test("token de michaelw que dura 300 segundos", function() {
          expect(payload.username).to.equal("michaelw");
          expect(payload.id).to.equal(2);
          expect(payload.exp - payload.iat).to.equal(300);
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.accessToken
        method: jsonq
      variable:
        name: accessToken
        scope: runtime
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.refreshToken
        method: jsonq
      variable:
        name: refreshToken
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`02-me.yml`
```yaml
info:
  name: 02 Quién soy
  type: http
  seq: 2

http:
  method: GET
  url: "{{baseUrl}}/auth/me"
  auth:
    type: bearer
    token: "{{accessToken}}"

runtime:
  scripts:
    - type: tests
      code: |-
        test("es michaelw y su rol es admin", function() {
          expect(res.body.id).to.equal(2);
          expect(res.body.role).to.equal("admin");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`03-wrong-password.yml`
```yaml
info:
  name: 03 Contraseña equivocada
  type: http
  seq: 3

http:
  method: POST
  url: "{{baseUrl}}/auth/login"
  body:
    type: json
    data: |-
      {
        "username": "{{username}}",
        "password": "otra"
      }

runtime:
  scripts:
    - type: tests
      code: |-
        test("DummyJSON responde 400 (lo correcto sería 401)", function() {
          expect(res.body.message).to.equal("Invalid credentials");
        });
  assertions:
    - expression: res.status
      operator: eq
      value: "400"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`04-refresh.yml`
```yaml
info:
  name: 04 Renovar
  type: http
  seq: 4

http:
  method: POST
  url: "{{baseUrl}}/auth/refresh"
  body:
    type: json
    data: |-
      {
        "refreshToken": "{{refreshToken}}",
        "expiresInMins": 5
      }

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "200"
  actions:
    - type: set-variable
      phase: after-response
      selector:
        expression: res.body.accessToken
        method: jsonq
      variable:
        name: accessToken
        scope: runtime

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

`05-me-again.yml`
```yaml
info:
  name: 05 Quién soy (token renovado)
  type: http
  seq: 5

http:
  method: GET
  url: "{{baseUrl}}/auth/me"
  auth:
    type: bearer
    token: "{{accessToken}}"

runtime:
  assertions:
    - expression: res.status
      operator: eq
      value: "200"

settings:
  encodeUrl: true
  timeout: 0
  followRedirects: true
  maxRedirects: 5
  forwardAuthorizationHeader: true
```

Ejecución completa (`bru run --env dev`):
```text
01-login (200 OK) - 210 ms
Payload: {"id":2,"username":"michaelw","email":"michael.williams@x.dummyjson.com","firstName":"Michael","lastName":"Williams","gender":"male","image":"https://dummyjson.com/icon/michaelw/128","iat":1793610794,"exp":1793611094}
Caduca: 2026-11-02T09:18:14.000Z
Tests
   ✓ token de michaelw que dura 300 segundos
Assertions
   ✓ res.status: eq 200
02-me (200 OK) - 152 ms
Tests
   ✓ es michaelw y su rol es admin
Assertions
   ✓ res.status: eq 200
03-wrong-password (400 Bad Request) - 155 ms
Tests
   ✓ DummyJSON responde 400 (lo correcto sería 401)
Assertions
   ✓ res.status: eq 400
04-refresh (200 OK) - 179 ms
Assertions
   ✓ res.status: eq 200
05-me-again (200 OK) - 163 ms
Assertions
   ✓ res.status: eq 200
📊 Execution Summary
┌───────────────┬──────────────┐
│ Metric        │    Result    │
├───────────────┼──────────────┤
│ Status        │    ✓ PASS    │
├───────────────┼──────────────┤
│ Requests      │ 5 (5 Passed) │
├───────────────┼──────────────┤
│ Tests         │     3/3      │
├───────────────┼──────────────┤
│ Assertions    │     5/5      │
├───────────────┼──────────────┤
│ Duration (ms) │     1905     │
└───────────────┴──────────────┘
```

**4. Permisos.**

`permisos-gimnasio.txt`
```text
QUIÉN PUEDE HACER QUÉ · API del gimnasio · soluciones

Endpoint                          Anónimo  Socio         Monitor           Recepción
                                           (MEMBER)      (TRAINER)         (STAFF)
--------------------------------  -------  ------------  ----------------  ---------
GET    /classes                   Sí       Sí            Sí                Sí
POST   /classes                   401      403           403               Sí
DELETE /classes/{id}              401      403           403               Sí
GET    /classes/{id}/bookings     401      403           Solo sus clases   Sí
POST   /bookings                  401      Solo para sí  403               Sí
PATCH  /bookings/{id}             401      Solo suyas    403               Sí
GET    /members/{id}              401      Solo el suyo  403               Sí

(Todas las rutas empiezan por /api/v1.)

Comprobaciones de propiedad (OWASP API1, BOLA)
- POST /bookings: el memberId del cuerpo debe ser el del token (si no, 403).
- PATCH /bookings/{id}: la reserva debe ser del socio del token.
- GET /classes/{id}/bookings: el monitor solo ve las clases que imparte.
```

**5. Preguntas.**
1. Puede hacer **todo lo que tú puedas** con el API (consultar tus datos, hacer peticiones en tu nombre) hasta que el token caduque, como mucho 30 minutos o lo que se pidiera en `expiresInMins`. Si además consigue el token de refresco, puede seguir obteniendo tokens nuevos. Lo evita **HTTPS**: el token viaja cifrado y no se puede leer en la red. DummyJSON lo cumple; el peligro está en usar `http://` o en una red con un certificado falso que el usuario acepta.
2. Porque con Basic la **contraseña** viaja en cada petición y el cliente tiene que guardarla para enviarla. En una herramienta interna con pocos usuarios se acepta; en una app pública, cada petición es una oportunidad de robo de la contraseña real, que además suele reutilizarse en otros sitios. Con tokens, la contraseña viaja una vez y lo que circula caduca pronto.
3. El cliente **puede** usarlo para mostrar u ocultar botones (es comodidad para el usuario), pero el **servidor** no puede fiarse de lo que haga el cliente: debe comprobar el rol y la propiedad en **cada** petición. Un atacante no usa tu app; llama al API directamente con curl. Es la diferencia entre API5 (comprobación de funciones) bien o mal resuelta.

---

## 03.07 · OpenAPI como contrato: diseñar antes de programar

### Resumen
- **OpenAPI** es el estándar para describir un API REST en un fichero YAML o JSON: endpoints, parámetros, cuerpos, respuestas, errores y seguridad. Lo leen personas y herramientas.
- **Diseño primero** (*design-first*): se escribe el contrato, se revisa y se prueba antes de programar. **Código primero** (*code-first*): se programa y el contrato se genera desde el código (springdoc, 08.01). En el curso diseñas aquí y generas allí, y comparas.
- Estructura: `openapi`, `info`, `servers`, `paths` (operaciones) y `components` (esquemas, parámetros, respuestas y seguridad reutilizables con `$ref`).
- Herramientas: **Swagger Editor** (escribir y ver), **Redocly CLI** (validar con `lint`) y **Prism** (servidor simulado que responde según el contrato).
- Con el contrato y el *mock*, quien programa el cliente puede empezar el mismo día, y tú tienes la especificación exacta de lo que debes programar.

### Teoría

**Qué es OpenAPI.** La *OpenAPI Specification* (antes llamada Swagger) es un formato estándar, mantenido por la OpenAPI Initiative, para describir APIs HTTP. Versiones:
- **3.0** (2017): la más extendida durante años.
- **3.1** (2021): se alinea con JSON Schema 2020-12 (por ejemplo, `type: [string, 'null']` para campos que admiten `null`).
- **3.2** (septiembre de 2025): añade, entre otras cosas, soporte para más métodos HTTP y mejoras en etiquetas. Las herramientas la están incorporando.

Usamos **3.1**: la admiten todas las herramientas del módulo y es la que puede generar springdoc en 08.01.

**¿Diseño primero o código primero?**

| | Diseño primero | Código primero |
|---|---|---|
| Orden | Contrato → revisión → código | Código → contrato generado |
| Ventaja | Se piensa el API antes de programarlo; cliente y servidor trabajan en paralelo con el *mock* | El contrato nunca se desfasa del código |
| Inconveniente | Hay que mantener el contrato al día | El diseño sale "de lo que se programó" |
| En el curso | Este módulo: `library-api.yaml` | 08.01: springdoc genera el contrato del código; se compara con este |

En la práctica profesional se combinan: se diseña el contrato, se programa y después se verifica que lo generado coincide con lo diseñado.

**Estructura de un documento OpenAPI.**

```yaml
openapi: 3.1.0            # versión de la especificación
info:                     # título, versión DEL API, descripción, licencia
  title: API de la biblioteca
  version: 1.0.0
servers:                  # dónde está desplegada
  - url: http://localhost:8080
paths:                    # una entrada por URI…
  /api/v1/books/{isbn}:
    get:                  # …y dentro, una operación por método
      operationId: getBook
      responses:
        '200': …          # una entrada por código de estado posible
        '404': …
components:               # piezas reutilizables, referenciadas con $ref
  schemas: …              # la forma de los JSON (Book, ProblemDetail…)
  parameters: …           # parámetros comunes (page, size, sort…)
  responses: …            # respuestas comunes (NotFound, BadRequest…)
  securitySchemes: …      # cómo se autentica (Bearer JWT)
```

**Las piezas que vas a usar.**
- **Operación**: `operationId` (nombre único; springdoc lo usa como nombre del método), `summary`, `tags` (para agrupar), `parameters`, `requestBody`, `responses` y `security`.
- **Parámetros**: `in: path | query | header`, con `required` y un `schema` (`type`, `pattern`, `minimum`, `maximum`, `default`).
- **Esquemas** (JSON Schema): `type: object` con `properties` y la lista `required`; tipos `string`, `integer`, `number`, `boolean`, `array` (con `items`); restricciones (`minLength`, `pattern`, `enum`, `format: date`). Son, en el fondo, la validación que harás con Bean Validation en el módulo 07.
- **`$ref`**: `$ref: '#/components/schemas/BookDetail'` reutiliza una definición. Evita copiar y mantiene el contrato coherente.
- **`example`**: ejemplos de valores. Documentan y, además, Prism los usa para responder.
- **Seguridad**: se define el esquema en `components.securitySchemes` (`type: http`, `scheme: bearer`, `bearerFormat: JWT`) y cada operación declara si lo necesita (`security: [bearerAuth: []]`) o si es pública (`security: []`).
- **Errores**: las respuestas 4xx usan `application/problem+json` con el esquema `ProblemDetail` de 03.03.

**Las herramientas.**

| Herramienta | Qué hace | Cómo se usa |
|---|---|---|
| Swagger Editor | Editor web con vista previa navegable (como Swagger UI) | Abre https://editor.swagger.io, pega o importa el YAML. Nada que instalar |
| Redocly CLI | Valida que el contrato es correcto y sigue buenas prácticas | `npx @redocly/cli lint library-api.yaml` |
| Prism | Levanta un servidor **simulado** (*mock*) que responde según el contrato y valida las peticiones | `npx @stoplight/prism-cli mock library-api.yaml` |
| Bruno | Puede **importar** el contrato y crear una colección con todas las peticiones | *Import Collection → OpenAPI* |

Swagger Editor guarda lo que escribes en el navegador; descarga el fichero (*File → Save as YAML*) y trabaja con él en tu carpeta.

### Código de ejemplo

**1. El contrato de los libros.** Recoge todo lo decidido en el módulo: URIs y métodos (03.02), códigos y Problem Details (03.03), paginación con el formato de Spring Data (03.04), versión en la ruta (03.05) y seguridad Bearer en las operaciones que modifican (03.06).

`library-api.yaml`
```yaml
openapi: 3.1.0
info:
  title: API de la biblioteca
  version: 1.0.0
  description: |
    Contrato del API REST de la biblioteca (módulo 03 de DWES).
    Se diseña antes de programar y se implementa con Spring Boot en el módulo 06.
  license:
    name: CC BY-SA 4.0
    url: https://creativecommons.org/licenses/by-sa/4.0/
servers:
  - url: http://localhost:8080
    description: Servidor local de desarrollo
tags:
  - name: books
    description: Catálogo de libros
paths:
  /api/v1/books:
    get:
      tags: [books]
      operationId: listBooks
      security: []
      summary: Lista los libros, filtrados, ordenados y paginados
      parameters:
        - name: author
          in: query
          description: Texto que debe contener el nombre del autor
          schema:
            type: string
          example: García Márquez
        - name: available
          in: query
          description: Solo libros con ejemplares disponibles
          schema:
            type: boolean
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/Size'
        - $ref: '#/components/parameters/Sort'
      responses:
        '200':
          description: Una página de libros (puede estar vacía)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookPage'
              example:
                content:
                  - isbn: '9788437604947'
                    title: Cien años de soledad
                    available: true
                  - isbn: '9788497592208'
                    title: Crónica de una muerte anunciada
                    available: true
                page:
                  size: 2
                  number: 0
                  totalElements: 5
                  totalPages: 3
        '400':
          $ref: '#/components/responses/BadRequest'
    post:
      tags: [books]
      operationId: createBook
      summary: Da de alta un libro
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BookRequest'
      responses:
        '201':
          description: Libro creado
          headers:
            Location:
              description: URL del libro creado
              schema:
                type: string
                format: uri
              example: http://localhost:8080/api/v1/books/9788437604947
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookDetail'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '409':
          $ref: '#/components/responses/Conflict'
  /api/v1/books/{isbn}:
    parameters:
      - $ref: '#/components/parameters/Isbn'
    get:
      tags: [books]
      operationId: getBook
      security: []
      summary: Detalle de un libro
      responses:
        '200':
          description: El libro
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookDetail'
        '404':
          $ref: '#/components/responses/NotFound'
    put:
      tags: [books]
      operationId: replaceBook
      summary: Reemplaza los datos de un libro
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BookRequest'
      responses:
        '200':
          description: Libro actualizado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookDetail'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
    delete:
      tags: [books]
      operationId: deleteBook
      summary: Da de baja un libro
      security:
        - bearerAuth: []
      responses:
        '204':
          description: Libro borrado (sin cuerpo)
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
  parameters:
    Isbn:
      name: isbn
      in: path
      required: true
      description: ISBN-13 del libro
      schema:
        type: string
        pattern: '^[0-9]{13}$'
      example: '9788437604947'
    Page:
      name: page
      in: query
      description: Número de página (empieza en 0)
      schema:
        type: integer
        minimum: 0
        default: 0
    Size:
      name: size
      in: query
      description: Elementos por página
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
    Sort:
      name: sort
      in: query
      description: 'Campo y dirección, por ejemplo title,asc'
      schema:
        type: string
        default: title,asc
  schemas:
    BookSummary:
      type: object
      required: [isbn, title, available]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        title:
          type: string
        available:
          type: boolean
    Author:
      type: object
      required: [id, name]
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
    BookDetail:
      type: object
      required: [isbn, title, author, available, copies]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        title:
          type: string
        author:
          $ref: '#/components/schemas/Author'
        publishedYear:
          type: integer
        pages:
          type: integer
        available:
          type: boolean
        copies:
          type: integer
          minimum: 0
        genres:
          type: array
          items:
            type: string
      example:
        isbn: '9788437604947'
        title: Cien años de soledad
        author:
          id: 12
          name: Gabriel García Márquez
        publishedYear: 1984
        pages: 493
        available: true
        copies: 3
        genres: [novela, realismo mágico]
    BookRequest:
      type: object
      required: [isbn, title, authorId, copies]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        title:
          type: string
          minLength: 1
          maxLength: 200
        authorId:
          type: integer
          format: int64
        publishedYear:
          type: integer
          maximum: 2100
        pages:
          type: integer
          minimum: 1
        copies:
          type: integer
          minimum: 0
        genres:
          type: array
          items:
            type: string
      example:
        isbn: '9788437604947'
        title: Cien años de soledad
        authorId: 12
        publishedYear: 1984
        pages: 493
        copies: 3
        genres: [novela, realismo mágico]
    PageMetadata:
      type: object
      required: [size, number, totalElements, totalPages]
      properties:
        size:
          type: integer
          minimum: 1
        number:
          type: integer
          minimum: 0
        totalElements:
          type: integer
          format: int64
          minimum: 0
        totalPages:
          type: integer
          minimum: 0
    BookPage:
      type: object
      required: [content, page]
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/BookSummary'
        page:
          $ref: '#/components/schemas/PageMetadata'
    ProblemDetail:
      type: object
      description: Error según RFC 9457
      properties:
        type:
          type: string
          format: uri-reference
          default: about:blank
        title:
          type: string
        status:
          type: integer
        detail:
          type: string
        instance:
          type: string
          format: uri-reference
        errors:
          type: array
          description: Solo en errores de validación
          items:
            type: object
            properties:
              field:
                type: string
              message:
                type: string
  responses:
    BadRequest:
      description: Petición no válida
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: https://api.biblioteca.example/problems/validation-error
            title: Datos no válidos
            status: 400
            detail: La petición tiene 1 campo con errores
            instance: /api/v1/books
            errors:
              - field: isbn
                message: debe tener 13 dígitos
    Unauthorized:
      description: Falta el token o no es válido
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: about:blank
            title: Unauthorized
            status: 401
            detail: Se necesita un token de acceso
            instance: /api/v1/books
    NotFound:
      description: El libro no existe
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: about:blank
            title: Not Found
            status: 404
            detail: No existe ningún libro con ISBN 9780000000000
            instance: /api/v1/books/9780000000000
    Conflict:
      description: Ya existe un libro con ese ISBN
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: https://api.biblioteca.example/problems/duplicate-isbn
            title: ISBN repetido
            status: 409
            detail: Ya existe un libro con ISBN 9788437604947
            instance: /api/v1/books
```

Ábrelo en Swagger Editor: a la derecha verás la documentación navegable, con los esquemas, los ejemplos y el candado en las operaciones protegidas.

**2. Validar el contrato.** Redocly aplica su conjunto de reglas `recommended`. En la misma carpeta, un fichero de configuración desactiva la regla que avisa de los servidores `localhost`, porque en desarrollo es lo que queremos.

`redocly.yaml`
```yaml
extends:
  - recommended
rules:
  # En desarrollo el servidor es localhost: no es un error
  no-server-example.com: off
```

`lint.sh`
```bash
npx @redocly/cli lint library-api.yaml
```

✅ **Si ves esto, vas bien:**
```text
validating library-api.yaml...
library-api.yaml: validated in 148ms
Woohoo! Your API description is valid. 🎉
```

**3. Simular el API.** Prism arranca un servidor en el puerto 4010 que responde según el contrato. Ejecútalo en un terminal y **déjalo abierto**; las peticiones, en otro terminal.

`mock.sh`
```bash
npx @stoplight/prism-cli mock library-api.yaml
```

```text
[CLI] …  awaiting  Starting Prism…
[CLI] ℹ  info      GET        http://127.0.0.1:4010/api/v1/books?author=Garc%C3%ADa%20M%C3%A1rquez&available=false&page=349&size=74&sort=aestas
[CLI] ℹ  info      POST       http://127.0.0.1:4010/api/v1/books
[CLI] ℹ  info      GET        http://127.0.0.1:4010/api/v1/books/9788437604947
[CLI] ℹ  info      PUT        http://127.0.0.1:4010/api/v1/books/9788437604947
[CLI] ℹ  info      DELETE     http://127.0.0.1:4010/api/v1/books/9788437604947
[CLI] ▶  start     Prism is listening on http://127.0.0.1:4010
```

Prism lista las operaciones del contrato con valores de ejemplo inventados para los parámetros, y queda escuchando.

**4. Listar y consultar.** Prism responde con los `example` del contrato.

`01-list.sh`
```bash
curl -i http://127.0.0.1:4010/api/v1/books
```

```text
HTTP/1.1 200 OK
Content-type: application/json
Date: Mon, 02 Nov 2026 09:26:56 GMT

{"content":[{"isbn":"9788437604947","title":"Cien años de soledad","available":true},{"isbn":"9788497592208","title":"Crónica de una muerte anunciada","available":true}],"page":{"size":2,"number":0,"totalElements":5,"totalPages":3}}
```

`02-get.sh`
```bash
curl -i http://127.0.0.1:4010/api/v1/books/9788437604947
```

```text
HTTP/1.1 200 OK
Content-type: application/json
Date: Mon, 02 Nov 2026 09:26:57 GMT

{"isbn":"9788437604947","title":"Cien años de soledad","author":{"id":12,"name":"Gabriel García Márquez"},"publishedYear":1984,"pages":493,"available":true,"copies":3,"genres":["novela","realismo mágico"]}
```

**5. Prism valida las peticiones contra el contrato.** Un ISBN de 3 dígitos no cumple el `pattern` del parámetro:

`03-get-isbn-invalido.sh`
```bash
curl -i http://127.0.0.1:4010/api/v1/books/123
```

```text
HTTP/1.1 422 Unprocessable Entity
content-type: application/problem+json
Date: Mon, 02 Nov 2026 09:26:58 GMT

{"type":"https://stoplight.io/prism/errors#UNPROCESSABLE_ENTITY","title":"Invalid request","status":422,"detail":"Your request is not valid and no HTTP validation response was found in the spec, so Prism is generating this error for you.","validation":[{"location":["path","isbn"],"severity":"Error","code":"pattern","message":"Request path parameter isbn must match pattern \"^[0-9]{13}$\""}]}
```

Prism lo detecta y responde con un **Problem Details** (`application/problem+json`, RFC 9457). Como la operación `getBook` no declara ninguna respuesta 400, Prism genera la suya, con 422. Es una pista: **al contrato le falta el 400** en `GET /books/{isbn}` (lo arreglarás en la práctica).

**6. Seguridad y validación del cuerpo.** Sin token, la operación protegida responde 401 con el ejemplo del contrato:

`04-post-sin-token.sh`
```bash
curl -i -X POST http://127.0.0.1:4010/api/v1/books -H "Content-Type: application/json" --data @book-new.json
```

```text
HTTP/1.1 401 Unauthorized
sl-violations: [{"location":["request"],"severity":"Error","code":401,"message":"Invalid security scheme used"}]
Content-type: application/problem+json
Date: Mon, 02 Nov 2026 09:26:59 GMT

{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Se necesita un token de acceso","instance":"/api/v1/books"}
```

Con token (Prism no comprueba que sea válido, solo que esté), pero con un cuerpo que incumple el esquema:

`book-invalid.json`
```json
{
  "isbn": "978-84",
  "title": "",
  "copies": -1
}
```

`05-post-invalido.sh`
```bash
curl -i -X POST http://127.0.0.1:4010/api/v1/books -H "Authorization: Bearer token-de-prueba" -H "Content-Type: application/json" --data @book-invalid.json
```

```text
HTTP/1.1 400 Bad Request
sl-violations: [{"location":["request","body"],"severity":"Error","code":"required","message":"Request body must have required property 'authorId'"},{"location":["request","body","isbn"],"severity":"Error","code":"pattern","message":"Request body property isbn must match pattern \"^[0-9]{13}$\""},{"location":["request","body","title"],"severity":"Error","code":"minLength","message":"Request body property title must NOT have fewer than 1 characters"},{"location":["request","body","copies"],"severity":"Error","code":"minimum","message":"Request body property copies must be >= 0"}]
Content-type: application/problem+json
Date: Mon, 02 Nov 2026 09:27:01 GMT

{"type":"https://api.biblioteca.example/problems/validation-error","title":"Datos no válidos","status":400,"detail":"La petición tiene 1 campo con errores","instance":"/api/v1/books","errors":[{"field":"isbn","message":"debe tener 13 dígitos"}]}
```

La cabecera `sl-violations` lista **los cuatro problemas reales** (falta `authorId`, ISBN con formato incorrecto, título vacío, ejemplares negativos). El cuerpo es el **ejemplo** de `BadRequest` del contrato: un *mock* no calcula, devuelve ejemplos.

**7. Crear y borrar.**

`book-new.json`
```json
{
  "isbn": "9788497592208",
  "title": "Crónica de una muerte anunciada",
  "authorId": 12,
  "publishedYear": 1981,
  "pages": 144,
  "copies": 2
}
```

`06-post.sh`
```bash
curl -i -X POST http://127.0.0.1:4010/api/v1/books -H "Authorization: Bearer token-de-prueba" -H "Content-Type: application/json" --data @book-new.json
```

```text
HTTP/1.1 201 Created
Location: http://localhost:8080/api/v1/books/9788437604947
Content-type: application/json
Date: Mon, 02 Nov 2026 09:27:02 GMT

{"isbn":"9788437604947","title":"Cien años de soledad","author":{"id":12,"name":"Gabriel García Márquez"},"publishedYear":1984,"pages":493,"available":true,"copies":3,"genres":["novela","realismo mágico"]}
```

`07-delete.sh`
```bash
curl -i -X DELETE http://127.0.0.1:4010/api/v1/books/9788437604947 -H "Authorization: Bearer token-de-prueba"
```

```text
HTTP/1.1 204 No Content
Date: Mon, 02 Nov 2026 09:27:03 GMT
```

201 con `Location` y 204 sin cuerpo, como dice el contrato. Fíjate en que el POST devuelve *Cien años de soledad* aunque enviamos *Crónica de una muerte anunciada*: el *mock* no guarda nada. Sirve para probar la **forma** del API (URIs, códigos, cabeceras, esquemas), no la lógica. La lógica la programarás en el módulo 06, y este contrato te dirá exactamente qué tiene que devolver.

| Error frecuente | Causa | Solución |
|---|---|---|
| `Can't resolve $ref` | El `$ref` apunta a un componente que no existe o está mal escrito | Copia el nombre exacto del componente (distingue mayúsculas) |
| `bad indentation of a mapping entry` | Sangría incorrecta en el YAML (un espacio de más o de menos) | En YAML la sangría es la estructura: usa siempre 2 espacios, nunca tabuladores |
| `Every operation must have a unique operationId.` | Dos operaciones con el mismo `operationId` | Un nombre distinto por operación: `listBooks`, `getBook`… |
| `listen EADDRINUSE: address already in use 127.0.0.1:4010` | Ya hay un Prism arrancado en otro terminal | Cierra el otro (`Ctrl+C`) o usa otro puerto: `-p 4011` |
| `curl: (7) Failed to connect to 127.0.0.1 port 4010 after 0 ms: Connection refused` | Prism no está arrancado o lo has cerrado | Arráncalo en otro terminal y déjalo abierto |
| `Route not resolved, no path matched` con 404 | La URI no está en el contrato (por ejemplo, `/api/v1/libros`) | Revisa la ruta; Prism solo conoce lo que está en `paths` |

### Práctica

Amplía el contrato de la biblioteca con los **préstamos**. Trabaja sobre una copia de `library-api.yaml` del ejemplo, con su `redocly.yaml`.

1. Añade la respuesta **400** a `GET /api/v1/books/{isbn}` y comprueba con Prism que ahora el ISBN no válido devuelve 400 en lugar de 422.
2. Añade la etiqueta `loans` y estas operaciones, todas protegidas con `bearerAuth`:
   1. `POST /api/v1/loans` (`createLoan`): cuerpo con `isbn` y `memberId`. Respuestas 201 (con `Location` y el préstamo), 400, 401, 404 (socio o libro inexistentes) y 409 (límite de préstamos o sin ejemplares).
   2. `GET /api/v1/loans/{id}` (`getLoan`): 200, 401 y 404.
   3. `PATCH /api/v1/loans/{id}` (`returnLoan`): cuerpo `{"status": "RETURNED"}` (el único valor permitido). 200 con el préstamo devuelto, 400, 401, 404 y 409 (ya estaba devuelto).
   4. `GET /api/v1/members/{id}/loans` (`listMemberLoans`): filtro opcional `status` (`ACTIVE`, `RETURNED` u `OVERDUE`), paginación y orden por defecto `dueDate,asc`. Responde con una página de préstamos.
3. Define los esquemas `LoanStatus` (enum), `LoanRequest`, `LoanStatusChange`, `Loan` (con la forma de `loan.json` de 02.04: `book` y `member` resumidos y `returnDate` que admite `null`) y `LoanPage`, y una respuesta `LoanConflict` con el ejemplo del límite de préstamos. Pon ejemplos para que Prism devuelva datos coherentes.
4. Valida con Redocly hasta que no haya errores ni avisos.
5. Arranca Prism y prueba: el ISBN no válido, crear un préstamo, consultarlo, devolverlo, devolverlo con `"status": "LOST"`, los préstamos activos del socio 7, y los préstamos del socio 7 **sin token**.
6. Responde:
   1. ¿Por qué `LoanStatusChange` solo admite `RETURNED` y no los tres valores de `LoanStatus`?
   2. Si no pones un ejemplo en `LoanPage`, Prism inventa los datos de la página. ¿Qué problema puede tener eso y cómo lo evita el contrato?
   3. ¿Qué ganas al compartir este fichero con quien vaya a programar el cliente (la web o la app) antes de tener el servidor?

> **Pista:** copia la estructura de las operaciones de libros y cambia lo necesario. Si Redocly avisa de `operation-4xx-response` o de `security-defined`, revisa que cada operación tenga al menos un 4xx y su `security`.

### Solución

**1 a 4. Contrato ampliado.** Las novedades respecto al ejemplo: el 400 en `getBook`, la etiqueta `loans`, las cuatro operaciones, el parámetro `LoanId`, los esquemas de préstamos (con ejemplos), la respuesta `LoanConflict` y, en `PageMetadata`, valores mínimos para que el *mock* no invente números negativos.

`library-api.yaml`
```yaml
openapi: 3.1.0
info:
  title: API de la biblioteca
  version: 1.0.0
  description: |
    Contrato del API REST de la biblioteca (módulo 03 de DWES).
    Se diseña antes de programar y se implementa con Spring Boot en el módulo 06.
  license:
    name: CC BY-SA 4.0
    url: https://creativecommons.org/licenses/by-sa/4.0/
servers:
  - url: http://localhost:8080
    description: Servidor local de desarrollo
tags:
  - name: books
    description: Catálogo de libros
  - name: loans
    description: Préstamos de libros a socios
paths:
  /api/v1/books:
    get:
      tags: [books]
      operationId: listBooks
      security: []
      summary: Lista los libros, filtrados, ordenados y paginados
      parameters:
        - name: author
          in: query
          description: Texto que debe contener el nombre del autor
          schema:
            type: string
          example: García Márquez
        - name: available
          in: query
          description: Solo libros con ejemplares disponibles
          schema:
            type: boolean
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/Size'
        - $ref: '#/components/parameters/Sort'
      responses:
        '200':
          description: Una página de libros (puede estar vacía)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookPage'
              example:
                content:
                  - isbn: '9788437604947'
                    title: Cien años de soledad
                    available: true
                  - isbn: '9788497592208'
                    title: Crónica de una muerte anunciada
                    available: true
                page:
                  size: 2
                  number: 0
                  totalElements: 5
                  totalPages: 3
        '400':
          $ref: '#/components/responses/BadRequest'
    post:
      tags: [books]
      operationId: createBook
      summary: Da de alta un libro
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BookRequest'
      responses:
        '201':
          description: Libro creado
          headers:
            Location:
              description: URL del libro creado
              schema:
                type: string
                format: uri
              example: http://localhost:8080/api/v1/books/9788437604947
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookDetail'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '409':
          $ref: '#/components/responses/Conflict'
  /api/v1/books/{isbn}:
    parameters:
      - $ref: '#/components/parameters/Isbn'
    get:
      tags: [books]
      operationId: getBook
      security: []
      summary: Detalle de un libro
      responses:
        '200':
          description: El libro
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookDetail'
        '400':
          $ref: '#/components/responses/BadRequest'
        '404':
          $ref: '#/components/responses/NotFound'
    put:
      tags: [books]
      operationId: replaceBook
      summary: Reemplaza los datos de un libro
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BookRequest'
      responses:
        '200':
          description: Libro actualizado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BookDetail'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
    delete:
      tags: [books]
      operationId: deleteBook
      summary: Da de baja un libro
      security:
        - bearerAuth: []
      responses:
        '204':
          description: Libro borrado (sin cuerpo)
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
  /api/v1/loans:
    post:
      tags: [loans]
      operationId: createLoan
      summary: Presta un libro a un socio
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoanRequest'
      responses:
        '201':
          description: Préstamo creado
          headers:
            Location:
              description: URL del préstamo creado
              schema:
                type: string
                format: uri
              example: http://localhost:8080/api/v1/loans/1024
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Loan'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
        '409':
          $ref: '#/components/responses/LoanConflict'
  /api/v1/loans/{id}:
    parameters:
      - $ref: '#/components/parameters/LoanId'
    get:
      tags: [loans]
      operationId: getLoan
      summary: Detalle de un préstamo
      security:
        - bearerAuth: []
      responses:
        '200':
          description: El préstamo
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Loan'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
    patch:
      tags: [loans]
      operationId: returnLoan
      summary: Registra la devolución del libro
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoanStatusChange'
      responses:
        '200':
          description: Préstamo devuelto
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Loan'
              example:
                id: 1024
                book:
                  isbn: '9788437604947'
                  title: Cien años de soledad
                member:
                  id: 7
                  name: Lucía Romero
                loanDate: '2026-10-20'
                dueDate: '2026-11-03'
                returnDate: '2026-11-02'
                status: RETURNED
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
        '409':
          $ref: '#/components/responses/LoanConflict'
  /api/v1/members/{id}/loans:
    get:
      tags: [loans]
      operationId: listMemberLoans
      summary: Préstamos de un socio
      security:
        - bearerAuth: []
      parameters:
        - name: id
          in: path
          required: true
          description: Identificador del socio
          schema:
            type: integer
            format: int64
          example: 7
        - name: status
          in: query
          description: Filtra por estado del préstamo
          schema:
            $ref: '#/components/schemas/LoanStatus'
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/Size'
        - name: sort
          in: query
          description: 'Campo y dirección, por ejemplo dueDate,asc'
          schema:
            type: string
            default: dueDate,asc
      responses:
        '200':
          description: Una página de préstamos (puede estar vacía)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoanPage'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
  parameters:
    Isbn:
      name: isbn
      in: path
      required: true
      description: ISBN-13 del libro
      schema:
        type: string
        pattern: '^[0-9]{13}$'
      example: '9788437604947'
    LoanId:
      name: id
      in: path
      required: true
      description: Identificador del préstamo
      schema:
        type: integer
        format: int64
      example: 1024
    Page:
      name: page
      in: query
      description: Número de página (empieza en 0)
      schema:
        type: integer
        minimum: 0
        default: 0
    Size:
      name: size
      in: query
      description: Elementos por página
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
    Sort:
      name: sort
      in: query
      description: 'Campo y dirección, por ejemplo title,asc'
      schema:
        type: string
        default: title,asc
  schemas:
    BookSummary:
      type: object
      required: [isbn, title, available]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        title:
          type: string
        available:
          type: boolean
    Author:
      type: object
      required: [id, name]
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
    BookDetail:
      type: object
      required: [isbn, title, author, available, copies]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        title:
          type: string
        author:
          $ref: '#/components/schemas/Author'
        publishedYear:
          type: integer
        pages:
          type: integer
        available:
          type: boolean
        copies:
          type: integer
          minimum: 0
        genres:
          type: array
          items:
            type: string
      example:
        isbn: '9788437604947'
        title: Cien años de soledad
        author:
          id: 12
          name: Gabriel García Márquez
        publishedYear: 1984
        pages: 493
        available: true
        copies: 3
        genres: [novela, realismo mágico]
    BookRequest:
      type: object
      required: [isbn, title, authorId, copies]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        title:
          type: string
          minLength: 1
          maxLength: 200
        authorId:
          type: integer
          format: int64
        publishedYear:
          type: integer
          maximum: 2100
        pages:
          type: integer
          minimum: 1
        copies:
          type: integer
          minimum: 0
        genres:
          type: array
          items:
            type: string
      example:
        isbn: '9788437604947'
        title: Cien años de soledad
        authorId: 12
        publishedYear: 1984
        pages: 493
        copies: 3
        genres: [novela, realismo mágico]
    PageMetadata:
      type: object
      required: [size, number, totalElements, totalPages]
      properties:
        size:
          type: integer
          minimum: 1
        number:
          type: integer
          minimum: 0
        totalElements:
          type: integer
          format: int64
          minimum: 0
        totalPages:
          type: integer
          minimum: 0
    BookPage:
      type: object
      required: [content, page]
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/BookSummary'
        page:
          $ref: '#/components/schemas/PageMetadata'
    LoanStatus:
      type: string
      enum: [ACTIVE, RETURNED, OVERDUE]
    LoanRequest:
      type: object
      required: [isbn, memberId]
      properties:
        isbn:
          type: string
          pattern: '^[0-9]{13}$'
        memberId:
          type: integer
          format: int64
      example:
        isbn: '9788437604947'
        memberId: 7
    LoanStatusChange:
      type: object
      required: [status]
      properties:
        status:
          type: string
          enum: [RETURNED]
      example:
        status: RETURNED
    Loan:
      type: object
      required: [id, book, member, loanDate, dueDate, status]
      properties:
        id:
          type: integer
          format: int64
        book:
          $ref: '#/components/schemas/BookRef'
        member:
          $ref: '#/components/schemas/MemberRef'
        loanDate:
          type: string
          format: date
        dueDate:
          type: string
          format: date
        returnDate:
          type: [string, 'null']
          format: date
        status:
          $ref: '#/components/schemas/LoanStatus'
      example:
        id: 1024
        book:
          isbn: '9788437604947'
          title: Cien años de soledad
        member:
          id: 7
          name: Lucía Romero
        loanDate: '2026-10-20'
        dueDate: '2026-11-03'
        returnDate: null
        status: ACTIVE
    BookRef:
      type: object
      required: [isbn, title]
      properties:
        isbn:
          type: string
        title:
          type: string
    MemberRef:
      type: object
      required: [id, name]
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
    LoanPage:
      type: object
      required: [content, page]
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/Loan'
        page:
          $ref: '#/components/schemas/PageMetadata'
      example:
        content:
          - id: 1024
            book:
              isbn: '9788437604947'
              title: Cien años de soledad
            member:
              id: 7
              name: Lucía Romero
            loanDate: '2026-10-20'
            dueDate: '2026-11-03'
            returnDate: null
            status: ACTIVE
        page:
          size: 20
          number: 0
          totalElements: 1
          totalPages: 1
    ProblemDetail:
      type: object
      description: Error según RFC 9457
      properties:
        type:
          type: string
          format: uri-reference
          default: about:blank
        title:
          type: string
        status:
          type: integer
        detail:
          type: string
        instance:
          type: string
          format: uri-reference
        errors:
          type: array
          description: Solo en errores de validación
          items:
            type: object
            properties:
              field:
                type: string
              message:
                type: string
  responses:
    BadRequest:
      description: Petición no válida
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: https://api.biblioteca.example/problems/validation-error
            title: Datos no válidos
            status: 400
            detail: La petición tiene 1 campo con errores
            instance: /api/v1/books
            errors:
              - field: isbn
                message: debe tener 13 dígitos
    Unauthorized:
      description: Falta el token o no es válido
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: about:blank
            title: Unauthorized
            status: 401
            detail: Se necesita un token de acceso
            instance: /api/v1/books
    NotFound:
      description: El libro no existe
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: about:blank
            title: Not Found
            status: 404
            detail: No existe ningún libro con ISBN 9780000000000
            instance: /api/v1/books/9780000000000
    Conflict:
      description: Ya existe un libro con ese ISBN
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: https://api.biblioteca.example/problems/duplicate-isbn
            title: ISBN repetido
            status: 409
            detail: Ya existe un libro con ISBN 9788437604947
            instance: /api/v1/books
    LoanConflict:
      description: El préstamo choca con el estado actual
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'
          example:
            type: https://api.biblioteca.example/problems/loan-limit-reached
            title: Límite de préstamos alcanzado
            status: 409
            detail: El socio 7 ya tiene 3 préstamos activos (máximo 3)
            instance: /api/v1/loans
```

`lint.sh`
```bash
npx @redocly/cli lint library-api.yaml
```

```text
validating library-api.yaml...
library-api.yaml: validated in 195ms
Woohoo! Your API description is valid. 🎉
```

**5. Pruebas con Prism** (con `mock.sh` arrancado en otro terminal).

```text
[CLI] …  awaiting  Starting Prism…
[CLI] ℹ  info      GET        http://127.0.0.1:4010/api/v1/books?author=Garc%C3%ADa%20M%C3%A1rquez&available=true&page=713&size=53&sort=comes
[CLI] ℹ  info      POST       http://127.0.0.1:4010/api/v1/books
[CLI] ℹ  info      GET        http://127.0.0.1:4010/api/v1/books/9788437604947
[CLI] ℹ  info      PUT        http://127.0.0.1:4010/api/v1/books/9788437604947
[CLI] ℹ  info      DELETE     http://127.0.0.1:4010/api/v1/books/9788437604947
[CLI] ℹ  info      POST       http://127.0.0.1:4010/api/v1/loans
[CLI] ℹ  info      GET        http://127.0.0.1:4010/api/v1/loans/1024
[CLI] ℹ  info      PATCH      http://127.0.0.1:4010/api/v1/loans/1024
[CLI] ℹ  info      GET        http://127.0.0.1:4010/api/v1/members/7/loans?status=OVERDUE&page=66&size=16&sort=volaticus
[CLI] ▶  start     Prism is listening on http://127.0.0.1:4010
```

`01-get-isbn-invalido.sh`
```bash
curl -i http://127.0.0.1:4010/api/v1/books/123
```

```text
HTTP/1.1 400 Bad Request
sl-violations: [{"location":["request","path","isbn"],"severity":"Error","code":"pattern","message":"Request path parameter isbn must match pattern \"^[0-9]{13}$\""}]
Content-type: application/problem+json
Date: Mon, 02 Nov 2026 09:27:11 GMT

{"type":"https://api.biblioteca.example/problems/validation-error","title":"Datos no válidos","status":400,"detail":"La petición tiene 1 campo con errores","instance":"/api/v1/books","errors":[{"field":"isbn","message":"debe tener 13 dígitos"}]}
```

Ahora responde **400** con el ejemplo del contrato.

`loan-new.json`
```json
{
  "isbn": "9788437604947",
  "memberId": 7
}
```

`02-post-loan.sh`
```bash
curl -i -X POST http://127.0.0.1:4010/api/v1/loans -H "Authorization: Bearer token-de-prueba" -H "Content-Type: application/json" --data @loan-new.json
```

```text
HTTP/1.1 201 Created
Location: http://localhost:8080/api/v1/loans/1024
Content-type: application/json
Date: Mon, 02 Nov 2026 09:27:12 GMT

{"id":1024,"book":{"isbn":"9788437604947","title":"Cien años de soledad"},"member":{"id":7,"name":"Lucía Romero"},"loanDate":"2026-10-20","dueDate":"2026-11-03","returnDate":null,"status":"ACTIVE"}
```

`03-get-loan.sh`
```bash
curl -i http://127.0.0.1:4010/api/v1/loans/1024 -H "Authorization: Bearer token-de-prueba"
```

```text
HTTP/1.1 200 OK
Content-type: application/json
Date: Mon, 02 Nov 2026 09:27:13 GMT

{"id":1024,"book":{"isbn":"9788437604947","title":"Cien años de soledad"},"member":{"id":7,"name":"Lucía Romero"},"loanDate":"2026-10-20","dueDate":"2026-11-03","returnDate":null,"status":"ACTIVE"}
```

`loan-return.json`
```json
{
  "status": "RETURNED"
}
```

`04-return.sh`
```bash
curl -i -X PATCH http://127.0.0.1:4010/api/v1/loans/1024 -H "Authorization: Bearer token-de-prueba" -H "Content-Type: application/json" --data @loan-return.json
```

```text
HTTP/1.1 200 OK
Content-type: application/json
Date: Mon, 02 Nov 2026 09:27:14 GMT

{"id":1024,"book":{"isbn":"9788437604947","title":"Cien años de soledad"},"member":{"id":7,"name":"Lucía Romero"},"loanDate":"2026-10-20","dueDate":"2026-11-03","returnDate":"2026-11-02","status":"RETURNED"}
```

`loan-return-invalid.json`
```json
{
  "status": "LOST"
}
```

`05-return-invalido.sh`
```bash
curl -i -X PATCH http://127.0.0.1:4010/api/v1/loans/1024 -H "Authorization: Bearer token-de-prueba" -H "Content-Type: application/json" --data @loan-return-invalid.json
```

```text
HTTP/1.1 400 Bad Request
sl-violations: [{"location":["request","body","status"],"severity":"Error","code":"enum","message":"Request body property status must be equal to one of the allowed values: RETURNED"}]
Content-type: application/problem+json
Date: Mon, 02 Nov 2026 09:27:15 GMT

{"type":"https://api.biblioteca.example/problems/validation-error","title":"Datos no válidos","status":400,"detail":"La petición tiene 1 campo con errores","instance":"/api/v1/books","errors":[{"field":"isbn","message":"debe tener 13 dígitos"}]}
```

`06-member-loans.sh`
```bash
curl -i "http://127.0.0.1:4010/api/v1/members/7/loans?status=ACTIVE" -H "Authorization: Bearer token-de-prueba"
```

```text
HTTP/1.1 200 OK
Content-type: application/json
Date: Mon, 02 Nov 2026 09:27:16 GMT

{"content":[{"id":1024,"book":{"isbn":"9788437604947","title":"Cien años de soledad"},"member":{"id":7,"name":"Lucía Romero"},"loanDate":"2026-10-20","dueDate":"2026-11-03","returnDate":null,"status":"ACTIVE"}],"page":{"size":20,"number":0,"totalElements":1,"totalPages":1}}
```

`07-member-loans-sin-token.sh`
```bash
curl -i http://127.0.0.1:4010/api/v1/members/7/loans
```

```text
HTTP/1.1 401 Unauthorized
sl-violations: [{"location":["request"],"severity":"Error","code":401,"message":"Invalid security scheme used"}]
Content-type: application/problem+json
Date: Mon, 02 Nov 2026 09:27:17 GMT

{"type":"about:blank","title":"Unauthorized","status":401,"detail":"Se necesita un token de acceso","instance":"/api/v1/books"}
```

**6. Preguntas.**
1. Porque el cliente solo puede **devolver** un préstamo. `ACTIVE` lo pone el servidor al crearlo y `OVERDUE` lo calcula el servidor cuando pasa la fecha. Si el esquema de la petición aceptara los tres, el contrato estaría diciendo que el cliente puede "reactivar" un préstamo o marcarlo como vencido. El esquema de la petición describe lo que el cliente **puede pedir**, no todos los valores que existen (es también una defensa contra API3).
2. Que los datos inventados pueden no tener sentido (en la prueba sin ejemplo salía `"totalElements": -9007199254740991`) y quien programa el cliente con el *mock* construye sobre datos imposibles. Lo evitan dos cosas: **restricciones** en el esquema (`minimum: 0`), que además documentan el API y serán validaciones en el servidor, y **ejemplos** realistas.
3. Que puede programar el cliente **en paralelo**, contra el *mock*, sabiendo exactamente qué URIs, códigos y JSON va a recibir. Los malentendidos ("¿la página empieza en 0 o en 1?", "¿el error trae la lista de campos?") se descubren antes de escribir el servidor, cuando corregirlos es barato. Y tú, al programar el servidor, tienes una especificación que cumplir y comprobar.

---

## Resumen del módulo

| Concepto | Lo que debes saber explicar | Dónde lo volveremos a ver |
|---|---|---|
| REST | Qué es, sus restricciones y por qué "sin estado" importa | Todo el curso; microservicios (14) |
| Recurso y representación | Colección, elemento y subrecurso; identificador natural o sustituto; un recurso, varias representaciones | DTOs (06.04), entidades (10.01) |
| Diseño de URIs | Sustantivos en plural, minúsculas, sin verbos, versión en la ruta | `@RequestMapping` y `@PathVariable` (06.02) |
| Métodos seguros e idempotentes | Qué significan, cuáles son y por qué importa al repetir peticiones | Mappings (06.02), reintentos (12.03 y 15.05) |
| Acciones no CRUD | Crear un recurso o cambiar su estado antes que un verbo en la URI | Préstamos en el P1 |
| Códigos de estado | Elegir el código en cada caso; 400 frente a 409; 401 frente a 403; búsqueda vacía = 200 | `ResponseEntity` (06.03), errores (07.05) |
| Problem Details (RFC 9457) | Campos, tipo `application/problem+json` y extensiones | `ProblemDetail` y `@RestControllerAdvice` (07.05) |
| Filtros, orden y paginación | Convenciones, offset/page/cursor, metadatos, cabecera `Link` y límites | `@RequestParam` (06.02), `Pageable` (10.05) |
| Versionado | Cambios que rompen y que no; estrategias; `Deprecation` y `Sunset` | Proyectos P2 y P3 |
| Seguridad | HTTPS, autenticación frente a autorización, Basic, API key, Bearer, JWT y OWASP API Top 10 | Spring Security y JWT (11) |
| OpenAPI | Estructura del contrato, `$ref`, esquemas, seguridad; diseño primero frente a código primero | springdoc y Swagger UI (08.01), colección Bruno (08.02) |
