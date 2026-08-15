# TFG — Juan Vázquez Longueira (UDC, Ingeniería de Software)

*Nota de mantenimiento de este documento: registra decisiones de arquitectura y su razonamiento (qué se decidió, qué se descartó y por qué) — no firmas de métodos, tipos concretos ni otros detalles que ya viven en el código y que se desincronizarían de él. Al añadir contenido nuevo, colocarlo dentro de la sección existente a la que pertenece temáticamente, no por defecto como sección nueva al final.*

*No es un log de cambios: no narrar qué se hizo en una sesión de trabajo concreta, ni enumerar campos o propiedades añadidos/renombrados en una entidad (eso ya lo tiene el historial de git), ni mencionar detalles de implementación como una ruta HTTP o un verbo concreto — nada de eso es una decisión. Cada entrada debe ser la decisión más la razón real que la motivó, de forma concisa, sin reexplorar alternativas que no aporten nada nuevo; si la razón real no está clara, no rellenarla con una plausible inventada a posteriori. Aplicar un patrón ya establecido en el proyecto no es una decisión nueva y no necesita entrada propia. Los estándares de cómo escribir código a nivel general (convenciones de código, no decisiones de arquitectura o de producto) van en `CLAUDE.md`, no aquí.*

## Problema
La información sobre dónde leer cómics/mangas en internet está dispersa, en formatos heterogéneos y sin garantías de calidad. No existe un punto centralizado que lo agregue.

## Solución
Aplicación web de indexación colaborativa para búsqueda, seguimiento y localización de fuentes de lectura de cómics.

## Capas de datos
- **Metadatos del cómic** (título, autor, género, estado, portada, nº capítulos…): obtenidos automáticamente desde fuentes externas. El usuario no los toca.
- **Fuentes de lectura** (sitio web donde se puede leer): aportación comunitaria. Requiere cuenta registrada.

## Funcionalidades

### Catálogo
- Búsqueda por título (original y sinónimos), género y tipo (manga, tebeo, cómic occidental…).
- Ficha por obra con metadatos completos.
- Navegación pública sin cuenta.
- Sin duplicados: cada cómic existe una sola vez. **Decidido a nivel de modelo de datos** — ver sección "Deduplicación y fusión de metadatos". Pendiente únicamente la estrategia de respaldo para cuando no hay cruce directo entre fuentes (ver esa misma sección).
- Los usuarios **no crean cómics**, solo aportan fuentes sobre existentes.
- **Fuente de datos para la búsqueda: decidido usar una única fuente principal** (no combinación multi-fuente), invocada a través del mismo puerto `ComicMetadataProvider` ya definido en Adaptadores de metadatos — esto la mantiene intercambiable si la fuente activa deja de estar disponible, sin necesidad de un patrón nuevo. *Razón:* combinar resultados paginados y rankeados de varias fuentes en una búsqueda en vivo introduce problemas de deduplicación de entidades, paginación inconsistente entre fuentes y latencia acumulada que no se justifican para el alcance de un TFG. Para el **detalle** de un cómic ya identificado sí se mantiene la combinación multi-fuente (ver Adaptadores de metadatos): ahí el problema es de fusión de campos sobre una misma entidad, no de fusión de conjuntos de resultados distintos.
  - **Decidido: cada resultado de búsqueda lleva consigo el identificador de qué fuente lo produjo** (`ComicMetadataResult`), puesto por el propio adaptador — no expuesto como un método aparte en el servicio de catálogo que devuelva "la fuente activa". *Razón:* un método así ataría la forma del servicio a la premisa de una única fuente activa, justo la premisa que este mismo punto ya señala como revisable si se combinan varias fuentes en la búsqueda. Que el dato viaje en cada resultado sigue siendo válido si eso cambia; un método aparte no.
- **Decidido: sin carga batch inicial.** La fuente principal se consulta en vivo en cada búsqueda. `Comic` se persiste en BBDD únicamente cuando el usuario entra al detalle de una obra concreta (patrón cache-aside). *Razón:* una carga batch requiere infraestructura adicional (job programado, gestión de volumen inicial) que no se justifica para el alcance del TFG. El cache-aside mantiene el catálogo local creciendo orgánicamente a medida que los usuarios lo usan, sin necesidad de una carga previa.
  - **Decidido: la materialización de un cómic (el efecto de escritura del cache-aside anterior) y su lectura una vez ya persistido son dos operaciones distintas, no una sola que haga ambas cosas.** *Razón:* leer un cómic ya persistido debe ser una operación segura e idempotente; materializarlo por primera vez no lo es (busca-o-crea `Comic`/`ComicMetadataEntry`, y de paso `Author`/`Genre`/`Tag` nuevos si hacen falta). Mezclarlas en una sola obliga a elegir entre romper esa propiedad o renunciar al cache-aside tal y como está decidido arriba. La separación se refleja también en el servicio de dominio: `CatalogService` (orquesta el `ComicMetadataProvider` activo — búsqueda y materialización) queda aparte de `ComicService` (solo depende de la persistencia, expone la lectura de un cómic ya existente) — mismo criterio de qué depende de una fuente externa y qué no, aplicado igual al servicio que a su vía de entrada.

### Fuentes colaborativas
- Por cada cómic, lista de sitios donde se puede leer.
- Metadatos por fuente: nombre en el sitio, enlace, capítulos disponibles, estado, idioma…
- Requiere cuenta registrada para aportar.
- **Decidido — idioma de `ComicReadingEntry` como `locale: string`, no como enum cerrado** (`domain.mmd` reflejaba antes `language: Enum` sin conjunto de valores decidido): mismo criterio y misma razón que `User.locale` (ver "Usuario: modelo de datos, registro e inicio de sesión").
- **Decidido — unicidad de `ComicReadingEntry` sobre (`comic_id`, `source_id`, `url`):** un mismo cómic puede tener varias entries en el mismo `ComicReadingSource` si el enlace difiere (p. ej. un sitio que reparte una obra en varias URLs), pero no se admite una propuesta duplicada exacta. Se descartó restringir a un único enlace por (`comic_id`, `source_id`) por resultar demasiado estricto para ese caso, y dejar sin restricción por permitir duplicados exactos sin necesidad.
- **Decidido — ciclo de revisión de `ComicReadingEntry`: `approve()`/`reject()` son una transición única y guardada** (`PENDING` → resuelto), no una operación libre — un segundo intento de revisar la misma entry falla explícitamente en vez de sobrescribir en silencio al revisor/decisión anterior. Corregir una decisión ya tomada (p. ej. revertir un `REJECTED` erróneo) queda deliberadamente fuera como una operación aparte, sin construir todavía por no tener consumidor real — ningún flujo de administración existe aún (ver Administración, *por concretar*). Solo se decidió que no comparte el guard de la transición inicial, no su forma.
- **Decidido — `ComicReadingSource` también es aportación comunitaria, no dato curado como `ComicMetadataSource`:** un usuario puede proponer un sitio nuevo al aportar una entry, no solo elegir uno ya registrado. Por eso tiene el mismo ciclo `PENDING`/`APPROVED`/`REJECTED` y las mismas columnas de auditoría (`created_at`/`updated_at`/`reviewed_at`/`reviewed_by`) que `ComicReadingEntry`, más `contributed_by`. Consecuencia directa: aprobar una `ComicReadingEntry` exige que su `ComicReadingSource` ya esté `APPROVED` — no tiene sentido validar el enlace de un sitio que en sí mismo no se ha verificado todavía.
- **Decidido — crear una fuente nueva es "crear o fallar", no "buscar o crear":** si ya existe una `ComicReadingSource` con esa URL, la propuesta falla en vez de devolver en silencio la existente. *Razón:* el usuario está pidiendo explícitamente registrar un sitio nuevo; reutilizar una ya existente sin decírselo le haría creer que registró algo cuando no fue así. El error expone el id de la fuente existente para que el cliente pueda reintentar con `submit()` sobre ese id en su lugar.
- **Decidido — identidad de una fuente nueva se comprueba por URL, no por el nombre que escribe el usuario:** dos usuarios pueden nombrar el mismo sitio de forma distinta, así que el nombre no sirve para deduplicar. La URL de la fuente se colapsa además a su origin (`scheme://host`, sin path/query/fragment) antes de comparar o guardar — así da igual qué página concreta haya pegado el usuario al proponer el sitio, ya que `ComicReadingSource.url` identifica el sitio entero, no una página. `ComicReadingEntry.url` sí conserva el path/query (identifica una página concreta), normalizado solo en case y barra final sobrante. *Pendiente anotado, no implementado:* si la normalización se vuelve más agresiva en el futuro (p. ej. quitar parámetros de tracking), perder la URL cruda tal y como la pegó el usuario sería una pérdida real de información — hoy no se guarda, solo la normalizada, porque el normalizado actual es lo bastante conservador para no tirar nada que importe.
- **Decidido — el icono de una fuente no lo aporta el usuario, se resuelve automáticamente:** mismo criterio que `title`/`available_chapters` en `ComicReadingEntry` (dato hidratable, no exigido en el formulario). Por defecto se asume la convención `/favicon.ico` del sitio; el mecanismo es un puerto (`ComicReadingSourceIconResolver`, detectado por dominio, mismo espíritu que `ComicReadingProvider`) para poder añadir resolutores específicos por sitio más adelante sin tocar el resto del flujo.
- **Decidido — listar las `ComicReadingEntry` de un cómic no oculta las `PENDING` por defecto:** si un usuario aporta una fuente y no la ve aparecer en ningún sitio, parece que la aportación no sirvió de nada. El filtrado por estado queda como opción del llamador, no como comportamiento forzado.
- **Decidido — un único endpoint para proponer una `ComicReadingEntry`, no uno por caso (fuente existente / fuente nueva):** el cliente manda `sourceId`, o `sourceName`+`sourceUrl`, mutuamente excluyentes. *Razón:* para el usuario es una sola acción ("leí esto en tal sitio") — partirla en dos endpoints solo trasladaría al cliente la decisión de cuál llamar, sin aportar nada.
- **Decidido — validación real de `locale`: idioma ISO 639-1 obligatorio, región ISO 3166-1 alpha-2 opcional** (`es`, `es-ES`). No existe una anotación estándar de Bean Validation para BCP 47, así que se implementó un validador propio. Cierra el punto que había quedado pospuesto hasta tener capa de DTOs.

### Seguimiento personal
- El usuario registra qué está leyendo o quiere leer, con control de progreso.
- **Granularidad del progreso: último capítulo leído** (`chapters: int` en `ReadingState`), incrementable y decrementable manualmente con controles +/-. No se registra el historial de capítulos leídos individualmente — pedirle al usuario que marque cada capítulo por separado se considera excesivo para el alcance del TFG. *Trade-off aceptado:* cómics con numeración no lineal (decimales, extras, prólogos) no encajan perfectamente en un entero, pero el modelo lo deja abierto a futuro si se implementa `ChapterReadingEntry` (ver Modelo de dominio).
- Requiere cuenta registrada.
- **Distinto de las notificaciones de nuevos capítulos** (ver sección siguiente): el seguimiento de progreso registra por qué capítulo va el usuario; las notificaciones son una suscripción aparte a una fuente concreta que puede activarse o desactivarse sin afectar al progreso registrado. Son dos conceptos independientes aunque puedan referenciar el mismo Comic/Entry.

### Notificaciones de nuevos capítulos *(por concretar — explorada en discusión de diseño, no forma parte del backlog formal todavía)*
- Pantalla que muestra, agrupados por día, los capítulos nuevos de los cómics que el usuario sigue para notificaciones.
- **Decidido — granularidad:** la suscripción se hace por `ComicReadingEntry` concreta (no por `Comic` agregado, ni por `ComicReadingSource` genérica). *Razón:* solo hay señal fiable de "capítulo nuevo" para Entries cuya Source tenga Provider — los datos de número de capítulos de fuentes de metadatos (AniList, MyAnimeList) no son fiables para obras en emisión. Seguir por Entry además resuelve el filtro de idioma/fuente preferida sin necesidad de un mecanismo aparte, ya que el idioma ya es un atributo de `ComicReadingEntry`. Se descarta explícitamente seguir una `ComicReadingSource` completa ("avísame de todo lo nuevo en este sitio, sea el cómic que sea"): no se considera una necesidad real de usuario.
- **Decidido — independencia del seguimiento de progreso:** es una entidad distinta de `ReadingState`, no una extensión suya. Reutiliza la entidad `Follow` ya prevista en el modelo de dominio (antes anotada como tentativa sin alcance definido; ver `domain.mmd`).
- **Pendiente — mecanismo de detección:** requiere un job periódico que revise las Entries con suscripción activa, en lugar del patrón cache-aside on-demand ya usado para metadatos. Esto convierte en bloqueante la estrategia de invalidación de `ComicReadingEntry`, ya señalada como pendiente en este documento.
- **Pendiente — rate limiting:** no es viable actualizar en una sola tanda todas las Entries seguidas de un sitio. Previsiblemente hará falta una estrategia por `ComicReadingSource`/Provider, en tandas periódicas (N entries por sitio y ejecución, con algún criterio de orden). No decidible hoy: depende de límites reales de cada sitio, que no se conocerán hasta que exista al menos un `ComicReadingProvider` implementado.
- **Pendiente — tecnología de programación del job:** por decidir cuando el resto del diseño esté más cerrado (candidatas: `@Scheduled` de Spring frente a Quartz; el patrón general del proyecto favorece evitar infraestructura adicional salvo necesidad demostrada).
- **Pendiente — log histórico:** la pantalla necesita poder agrupar capítulos detectados por fecha, lo cual requiere que el dato de "capítulo detectado" no se sobrescriba. Probablemente cubierto por `ChapterReadingEntry` (entidad aún tentativa en el modelo — ver `domain.mmd`), con un campo de fecha de publicación/detección por capítulo, en vez de una entidad de log separada.

### Moderación *(por concretar)*
- Los usuarios pueden valorar fuentes aportadas: correcto, incorrecto, roto, spam…
- Requiere cuenta registrada.

### Administración *(por concretar)*
- Acceso total a todas las entidades del sistema, incluyendo cuentas de usuario. Alcance exacto de las operaciones por concretar.
- **Decidido — revisar (`approve`/`reject`) una `ComicReadingEntry` exige rol `ADMIN`:** primera regla de autorización por rol del proyecto (`hasRole(...)` en `SecurityConfig`), apoyada en el claim `role` que el JWT ya incluye desde su emisión. El resto del alcance de administración sigue sin concretar.

## Arquitectura de adaptadores

Patrón común para dos tipos de integraciones externas. En ambos casos se define una interfaz y se implementa por cada fuente/sitio concreto. Al menos un adaptador concreto de cada tipo será implementado en el TFG.

**Decidido: la URL base de la API externa de cada adaptador no se hardcodea, pero tampoco exige configuración obligatoria.** Valor por defecto embebido en el código, overrideable desde fuera sin recompilar (p. ej. `@Value("${provider.api.base-url:https://valor-por-defecto}")`). *Razón:* la URL de una API de terceros puede cambiar, o interesar apuntar a una instancia propia (p. ej. Jikan permite auto-hospedarse) sin perder el autocontenido por defecto. Ejemplo: `TenraiComicMetadataProvider`.

### Adaptadores de metadatos
- Obtienen información del cómic desde fuentes externas (AniList, MangaDex…).
- Combinables con prioridad configurable para completar información. **Aplica al detalle de un cómic ya identificado** (fusión de campos sobre una misma entidad); para la búsqueda de catálogo se usa una única fuente principal — ver sección Catálogo — por ser un problema distinto (fusión de conjuntos de resultados, no de campos). Modelo de datos de la fusión y de la deduplicación: ver sección "Deduplicación y fusión de metadatos".
- Estrategia: on-demand inicialmente, batch periódico a futuro.

### Adaptadores de fuentes de lectura
- Dado un enlace aportado por un usuario, extraen información del sitio: nombre del cómic en ese sitio, capítulos disponibles, estado…
- Detección del adaptador por dominio.
- Si no hay adaptador para un sitio, se almacena el enlace tal cual.

## Deduplicación y fusión de metadatos

Hay dos cosas que se mantenían mezcladas en redacciones anteriores de este documento y que conviene separar:

- **Trazabilidad por fuente:** cada fuente de metadatos (AniList, MangaDex…) puede traer datos distintos para la misma obra, o no traerlos en absoluto. Se guardan por separado, etiquetados con su procedencia. No es duplicación — es información distinta que debe poder atribuirse a su origen.
- **Fusión por prioridad:** ya decidida en Adaptadores de metadatos — los datos por fuente se combinan según un orden de prioridad configurable, completando huecos sin sobrescribir el valor de una fuente de mayor prioridad.

**Modelo de datos — decidido:**
- `Comic` (entidad canónica, ya existente en `domain/entities/`): guarda los valores **efectivos**, ya fusionados según prioridad, de los campos que el catálogo necesita mostrar y buscar (título, género, tipo, estado, portada, sinopsis…). Se recalculan cuando cambian los datos de alguna fuente o la prioridad configurada — no es una tabla de identidad vacía, es una vista materializada de la fusión.
- **`ComicMetadataSource`** (nombre definitivo — ver "Convención de nombres de entidades y adaptadores"): el sitio de metadatos en sí (AniList, MangaDex…), como entidad propia.
- **`ComicMetadataEntry`** (nombre definitivo, antes "FuenteMetadata" — ver "Convención de nombres de entidades y adaptadores"): una fila por (`comic_metadata_source_id`, id externo, comic_id), con restricción de unicidad sobre (`comic_metadata_source_id`, id externo). Guarda el dato crudo normalizado tal como lo devuelve esa fuente. Nombrada por simetría con `ComicReadingEntry`, ya existente. **Nota de migración de modelo:** al pasar `ComicMetadataSource` a ser una entidad real, la restricción de unicidad deja de apoyarse en un valor de fuente suelto (string/enum) y pasa a ser una FK a `ComicMetadataSource`.
- La equivalencia entre dos fuentes para el mismo cómic **no se guarda como una tabla de pares "id↔id"** — se deduce porque ambas filas de `ComicMetadataEntry` apuntan al mismo `comic_id`. Se descarta explícitamente el diseño de pares sueltos porque, con tres o más fuentes, introduce un problema de cierre transitivo (A≈B y B≈C conocidos, pero A≈C no registrado directamente) que el modelo anclado al Comic no tiene.
- **Pendiente:** `ComicMetadataEntry`, tal y como está modelada hoy, no tiene columnas para el dato crudo por campo (título, sinopsis, géneros…) — solo enlaza (`source_id`, `external_id`) → `comic_id`. Esto basta para la deduplicación (paso 1 de "Proceso de alta de una fuente nueva", abajo) pero no para la fusión por prioridad real descrita arriba: con un único provider activo no hay nada que fusionar todavía, así que el hueco no se nota. Antes de añadir un segundo provider habrá que decidir cómo y dónde se guarda el valor crudo por fuente y por campo.

**Proceso de alta de una fuente nueva — orden de resolución, decidido:**
1. Si el id externo ya está registrado en `ComicMetadataEntry`, se reutiliza su `comic_id` — no se crea un Comic nuevo.
2. Si no, se comprueba si la fuente expone una referencia cruzada nativa hacia una fuente ya conocida. Caso concreto verificado: la API de MangaDex expone un campo de enlaces que puede incluir directamente el ID de AniList de la obra.
3. Si tampoco hay referencia cruzada nativa, se puede consultar Wikidata, que mantiene propiedades de identificador estructuradas para varias fuentes de manga (AniList, MyAnimeList, MangaUpdates…). Cobertura parcial — depende de que la obra tenga ficha en Wikidata y esté enlazada. **No verificado para fuentes de cómic occidental** (Comic Vine, Grand Comics Database…), que usan un ecosistema de identificadores distinto.
4. **Pendiente, descartado a propósito por no ser necesario ahora:** estrategia de respaldo para cuando ninguno de los pasos anteriores da correspondencia (matching manual, fuzzy por título/autor, o tratar como cómic nuevo hasta una resolución posterior).

**Búsqueda de catálogo vs. ficha de detalle — decidido:** al filtrar por género/tipo/título, la coincidencia se evalúa contra el valor de **cualquier** fuente (unión), no solo el valor fusionado por prioridad — para no ocultar resultados solo porque la fuente de mayor prioridad no incluya una etiqueta que sí tiene otra. La ficha de detalle muestra el valor fusionado por prioridad. La prioridad es **global** (misma para todos los usuarios y todos los campos). *Mejora futura, no decidida:* prioridad configurable por usuario — se valoró y se descarta por ahora; el modelo ya guarda los datos crudos por fuente, así que esta puerta queda abierta sin coste arquitectónico si se decide más adelante.

**Acoplamiento con la caché de adaptadores de metadatos (ver Decisiones técnicas relevantes):** cuando el TTL pasivo ya decidido provoque un refresco de los datos de una fuente, ese refresco debe disparar también un recálculo de los valores fusionados en `Comic`. No es una decisión nueva sobre el mecanismo de caché — es una dependencia entre ambas capas que conviene dejar anotada.

## Convención de nombres de entidades y adaptadores

Nombres definitivos (sustituyen a "FuenteMetadata"/"FuenteLectura", que eran informales y provisionales):

| Concepto | Nombre definitivo | Antes (informal) |
|---|---|---|
| Sitio de metadatos en sí (AniList, MangaDex…), como entidad propia | `ComicMetadataSource` | no existía como entidad |
| Registro por-cómic-por-fuente de metadatos (dato crudo normalizado) | `ComicMetadataEntry` | `FuenteMetadata` |
| Sitio de lectura en sí (scans), como entidad propia | `ComicReadingSource` | no existía como entidad |
| Registro por-cómic-por-sitio de lectura (enlace, capítulos, estado, idioma) | `ComicReadingEntry` | `FuenteLectura` |
| Puerto de metadatos | `ComicMetadataProvider` | `MetadataProvider` |
| Puerto de fuentes de lectura | `ComicReadingProvider` | `ReadingSourceAdapter` |
| Implementación concreta de metadatos | `[Sitio]ComicMetadataProvider` (ej. `AniListComicMetadataProvider`, `MangaDexComicMetadataProvider`) | `AniListMetadataAdapter`, `MangaDexMetadataAdapter` |
| Implementación concreta de lectura | `[Sitio]ComicReadingProvider` | implementaciones sin nombre fijado |

**Razones de la convención:**

- **`Source` vs. `Entry` separa dos conceptos que antes compartían nombre bajo "fuente":** "fuente" se usaba de forma ambigua tanto para el sitio en sí (AniList, un sitio de scans) como para el registro que liga ese sitio con un cómic concreto. `Source` queda reservado para el sitio como entidad propia; `Entry` para el registro por-cómic. Esto obliga a modelar `ComicMetadataSource`/`ComicReadingSource` como entidades reales (antes el "sitio" de metadatos no existía como tabla, era un valor suelto dentro de `FuenteMetadata`), no solo a renombrar.
- **El prefijo `Comic` tiene dos motivaciones distintas según la entidad, no una sola:** en `ComicMetadataEntry`/`ComicReadingEntry` significa "vinculado a un cómic concreto" (la fila referencia un `comic_id`). En `ComicMetadataSource`/`ComicReadingSource` **no** hay vínculo a un cómic concreto — el prefijo ahí señala solo "del dominio de cómics" (p. ej. "fuente de metadata de cómics" en general, no de una obra en particular). Se deja anotado explícitamente para que no se asuma que una fila de `ComicMetadataSource` está atada a un cómic.
- **Se descartaron los términos `Port` y `Adapter` para nombrar los conceptos genéricos** (puerto e implementación de Ports & Adapters), reservándolos para referirse al patrón en sí, y se usó en su lugar un nombre concreto del dominio: `Provider`.
- **Convención puerto/implementación con `Provider`:** el puerto lleva el nombre genérico (`ComicMetadataProvider`), y cada implementación concreta antepone el nombre de la fuente al mismo nombre del puerto (`MangadexComicMetadataProvider`), en vez de usar un sufijo distinto tipo `...Adapter`. Es un patrón de nombrado extendido en Java/Spring (p. ej. `UserRepository` → `JpaUserRepository`; `PaymentGateway` → `StripePaymentGateway`): prefijo + mismo sufijo, en vez de sufijo distinto para cada nivel.
- **Excepción a esta convención:** cuando una fuente no expone API pública propia y se accede vía una API de terceros con nombre propio, la implementación se nombra según esa API, no según el sitio — caso de MyAnimeList, implementado como `TenraiComicMetadataProvider` (antes `JikanComicMetadataProvider`, hasta que la API Jikan se volvió inestable) en vez de `MyAnimeListComicMetadataProvider`. El `slug` de `ComicMetadataSource` sigue identificando el sitio, no la API usada para acceder a él.
- **Se corrige una inconsistencia previa del documento:** antes, el puerto del lado de lectura ya se llamaba `ReadingSourceAdapter` (con sufijo "Adapter", atípico para un puerto — rompía la convención que sí seguía el lado de metadatos, donde el puerto era `MetadataProvider` y solo las implementaciones llevaban "Adapter"). Con `ComicReadingProvider` como nuevo nombre del puerto, ambos lados (metadatos y lectura) siguen ahora el mismo criterio.

## Modelo de dominio

El diagrama de clases de dominio se encuentra en `docs/domain.mmd`. En él se incluyen descripciones de las entidades cuyo propósito no es evidente por su nombre.

## Roles de usuario

- **Visitante:** puede navegar y consultar el catálogo.
- **Usuario registrado:** puede además aportar fuentes y valorar las existentes.
- **Administrador:** acceso total a todas las entidades del sistema, incluyendo cuentas de usuario. Alcance exacto de las operaciones por concretar.

## Usuario: modelo de datos, registro e inicio de sesión

- **Tabla `app_user`, no `user`:** `user` es palabra reservada en PostgreSQL (verificado: `CREATE TABLE user (...)` falla sin comillas). Se descarta usarla entrecomillada — forzaría entrecomillar la tabla en todo el SQL y el mapeo JPA de por vida de la tabla, sin ninguna ventaja a cambio.
- **`created_at`/`updated_at` añadidos a `User`** (gestionados por Hibernate vía `@CreationTimestamp`/`@UpdateTimestamp`, no a mano): pensados como estándar de auditoría a futuro, con valor especial en una entidad como `User` (altas, bajas, cambios de cuenta).
- **`locale`: string libre con formato de locale estándar (BCP 47, p. ej. `es-ES`, `en-US`), no un enum cerrado.** *Razón:* soportar cualquier idioma a futuro sin migrar el esquema cada vez que se añada uno. Alternativas descartadas: enum cerrado `ES`/`EN`, o `ES`/`EN`/`GL` (coherente con que el TFG es de la UDC).
- **Longitud mínima de contraseña: regla de dominio (`UserService`), no de DTO.** Es una regla de seguridad, no una restricción de forma del *payload* — debe cumplirse sin importar la vía de entrada (REST hoy, cualquier otra en el futuro).
- **Hash de contraseña: puerto de dominio (`PasswordHasher`), no un tipo de Spring Security filtrado directamente en el dominio.** Implementado como adaptador *driven* (`BCryptPasswordHasher`), fuera de `adapter/rest/security/` — lo específico de REST (filtros, CORS) va ahí; lo que el dominio necesita como capacidad (hashing, y JWT — ver más abajo) se modela como puerto+adaptador aparte, hermano de `persistence/`.
- **Política de acceso por defecto: `denyAll()`, no `authenticated()`, para todo lo que no sea `/api/auth/**`.** *Razón:* fuerza a declarar explícitamente qué rutas son accesibles y con qué requisito, en vez de asumir por defecto que "cualquier usuario autenticado" basta — postura más estricta (denegar por defecto, permitir explícitamente) que además evita, hoy, que el resto de la aplicación quede accesible de facto a través del usuario en memoria que Spring Boot autogenera automáticamente mientras no exista un mecanismo de autenticación real.
- **Formato de respuesta de error: RFC 9457**, decidido. RFC 9457 ("Problem Details for HTTP APIs") obsoleta a la RFC 7807 anterior del mismo nombre, manteniendo el mismo formato de cuerpo (`type`, `title`, `status`, `detail`, `instance`) y tipo de contenido `application/problem+json` — es, a día de hoy, el estándar vigente para errores HTTP estructurados, no una elección arbitraria. Se usa el tipo `ProblemDetail` ya incluido en Spring Framework 6+ (sin librería adicional), habilitado globalmente con `spring.mvc.problemdetails.enabled=true` — esto cubre automáticamente los fallos de Bean Validation con el mismo formato, sin necesitar un `@ExceptionHandler` propio para ellos. Se descarta explícitamente definir un DTO de error propio: sería reinventar un estándar que el framework ya soporta de forma nativa.
- **`type` de `ProblemDetail`: identificador propio y estable por excepción (`urn:problem-type:...`, constantes en `ProblemType`), no el valor por defecto de Spring.** *Razón:* el status HTTP por sí solo es ambiguo para el cliente en cuanto dos excepciones de dominio distintas comparten status — p. ej. los dos 409 de registro (`UsernameAlreadyExistsException`/`EmailAlreadyExistsException`) o un 400 que puede ser tanto `WeakPasswordException` como un fallo de forma de Bean Validation. Detectado en la práctica al implementar el formulario de registro: el frontend intentaba adivinar cuál de los dos había ocurrido a partir del status code, una asunción frágil que ya llevaba a mostrar el error en el campo equivocado. Cada excepción pasa a tener su propio `@ExceptionHandler` (antes agrupados por status compartido) para poder fijar un `type` distinto, y el frontend compara contra ese valor en vez de contra el status.
- **Firma del JWT: HS256 (HMAC-SHA256) con secreto simétrico, no RSA/EC/Ed25519.** *Razón:* la ventaja de una clave asimétrica es separar quién puede firmar de quién puede verificar — no aplica aquí, donde el mismo backend emite y valida sus propios tokens (mismo dominio de confianza); comprometer ese backend compromete la firma da igual el tipo de clave. Comprobado además, no asumido, que el soporte de conveniencia de `spring-security-oauth2-jose` (`NimbusJwtEncoder`/`NimbusJwtDecoder`) tiene huecos reales para EC/Ed25519 — el lado de decodificación solo trae builder dedicado para RSA y para clave simétrica — mientras que HMAC es la única vía totalmente soportada en ambos lados sin bajar al `JWKSource` genérico de Nimbus. Ed25519 en concreto es criptográficamente más robusto (firma determinista, sin el riesgo de reutilización de nonce que tiene ECDSA) pero esa ventaja no compensa el hueco de soporte para el caso de uso actual.
- **Refresh token: opaco (no JWT), hasheado (SHA-256) en BBDD, rotado en cada uso.** *Razón:* un JWT de refresco sin estado no se puede revocar sin añadir de todos modos una blocklist — pierde en la práctica la ventaja de no tener estado que se busca al usar JWT. La rotación en cada uso permite detectar robo de token: si el mismo token ya rotado se reutiliza, la petición falla, delatando que hay una copia filtrada circulando.
- **Refresh token viaja como cookie `HttpOnly` + `Secure` + `SameSite=Lax`, nunca en el body JSON de la respuesta.** *Razón:* un token de vida larga accesible desde JavaScript es robable por XSS; `HttpOnly` lo hace inalcanzable desde JS. `SameSite=Lax` y no `Strict` porque el modelo de despliegue (dominio de frontend/backend) sigue sin decidir (ver Modelo de despliegue) — `Strict` podría romper la cookie si acaban siendo dominios distintos; a revisar cuando se cierre esa decisión.
- **Respuesta de login/refresh en formato estándar OAuth 2.0** (`access_token`, `token_type: "Bearer"`, `expires_in` en segundos — RFC 6749 §5.1), vía `@JsonNaming` snake_case sobre el DTO en vez de nombrar los campos a mano. No incluye `refresh_token`: ese va solo por cookie, por el punto anterior.
- **Access token en el frontend: solo en memoria (variable de módulo en `common/api/`), nunca en `localStorage`/`sessionStorage`.** *Razón:* a diferencia del refresh token, el access token sí tiene que ser legible desde JS (para adjuntarlo como cabecera `Authorization`), así que `HttpOnly` no es aplicable — pero eso no obliga a persistirlo. Guardarlo solo en memoria evita que quede expuesto a un `localStorage.getItem` desde un XSS o accesible tras cerrar la pestaña; el coste es que se pierde en un refresco de página (F5), lo cual se resuelve pidiendo uno nuevo contra `/api/auth/refresh` al arrancar la app usando la cookie del refresh token.
- **Estado de sesión en el frontend: React Context (`AuthProvider`/`useAuth` en `common/components/`), no una librería de estado global.** Al montarse llama a `GET /api/auth/me`, que gracias al mecanismo del punto anterior restaura la sesión sola si existía una cookie de refresh válida, sin lógica de arranque adicional. Expone el usuario autenticado (`null` si no hay sesión) y un `isLoading` para esa ventana inicial, usado p. ej. para no mostrar el enlace de "Iniciar sesión" en el `Header` ni antes de saber si hay sesión ni cuando ya la hay. *Razón para no usar Redux/Zustand:* el estado de sesión es, hoy, el único estado verdaderamente global de la aplicación; una librería añadiría una dependencia y un patrón nuevo para un caso que Context ya cubre sin coste.
- **Recordarme: decide si el refresh token es persistente o de sesión, tanto en la cookie como en el propio backend.** *Razón:* el `Max-Age` de la cookie solo lo hace cumplir el navegador — si el valor real del token se filtra (red sin TLS, logs...) y se reproduce directamente contra la API sin pasar por el navegador, el `Max-Age` no protege nada. Por eso, además de omitir `Max-Age` en la cookie cuando no se marca "recordarme" (se borra al cerrar el navegador), el `expiresAt` guardado en BBDD para ese `RefreshToken` también es corto (24h) en vez de los 30 días del caso recordado — el límite real de un token robado lo pone el servidor, no el navegador. `rememberMe` se persiste en la propia fila de `RefreshToken` (no en una cookie complementaria, alternativa valorada y descartada) para que sobreviva a la rotación: `UserService.refresh` lo lee del token ya guardado y lo traslada al siguiente, sin que el cliente tenga que volver a indicarlo en cada refresco.
- **Quién emite un token decide y comunica su propia expiración, en vez de que cada capa relea la misma config por separado.** Antes, `UserService`, `NimbusJwtIssuer` y `AuthController` leían independientemente las mismas propiedades (`jwt.access-token-expiration-minutes`, `jwt.refresh-token-expiration-days`) para llegar al mismo número por tres vías distintas — riesgo de desincronización si una cambia sin la otra. `JwtIssuer.issue(...)` devuelve `AccessToken(value, expiresAt)` en vez de un `String` suelto, y `TokenPair` lleva los `Instant` de expiración de ambos tokens; `AuthController` calcula `expires_in` y el `Max-Age` de la cookie a partir de esos valores, sin `@Value` propios para esas duraciones. Alternativa descartada: que `TokenPair` llevase la propia entidad `RefreshToken` en vez de campos planos — la entidad no guarda el valor en claro del token (solo su hash), así que no sustituye el campo `String` de todas formas, y expone más forma de persistencia de la necesaria en el tipo de retorno de un servicio de dominio.

## Metodología

Ágil con iteraciones cortas, precedidas de un análisis y diseño global inicial (este marco general se mantiene; lo que estaba pendiente era el framework concreto de las iteraciones).

**Decisión: Scrum simplificado**, adaptado a desarrollo en solitario. Eventos por iteración:

- **Planning:** selección del backlog de la iteración, incluyendo la investigación y el análisis necesarios para acotar el alcance. *Razón:* al ser una sola persona, no hay reparto de tareas que justifique mantener "investigación y análisis" como fase separada del resto; se concentran en Planning porque es ahí donde se decide qué entra en la iteración. Es esperable que surja investigación o análisis puntual adicional durante el Increment ante imprevistos de diseño o implementación — esto se reconoce como excepción normal, no como inconsistencia del esquema.
- **Increment:** diseño + desarrollo + pruebas de las historias seleccionadas en Planning.
- **Review + Retrospective:** al final de la iteración, en una sola sesión pero como **dos salidas separadas** (qué se construyó vs. cómo se trabajó). *Razón:* al ser una sola persona hay poca discusión que requiera separar las sesiones, pero la memoria del TFG necesita poder evidenciar ambas cosas por separado.
- **Daily:** descartada explícitamente. *Razón:* no hay equipo con quien sincronizar.

**Timeboxing:** 1 semana por iteración. Fijo dentro de un incremento en curso; puede ajustarse de un incremento a otro si la duración resulta insuficiente o excesiva. *Razón:* evitar que el timebox se convierta en una variable libre a mitad de trabajo (lo que suele invalidar la práctica); se revisa solo entre incrementos porque 1 semana es una estimación inicial todavía sin validar con datos reales.

## Gestión de tareas

**Decisión: Jira (plan Free, proyecto de tipo team-managed).** *Razón:* el team-managed (antes "next-gen") no requiere administrador de Jira para crearse ni configurarse, y permite activar o desactivar funciones ágiles (sprints, backlog, estimaciones) según necesidad — ajustado a un proyecto de una sola persona, frente a la configuración más rígida del company-managed. Frente a GitHub Projects, se prioriza tener informes ágiles automáticos de fábrica (burndown, velocity): conectan directamente con una decisión pendiente de la metodología — el timeboxing de 1 semana está marcado como "estimación inicial sin validar con datos reales" (ver Metodología), y esos informes son la fuente de esos datos sin trabajo manual añadido.

**Integración con GitHub:** app oficial "GitHub for Jira" (gratuita, instalación de pocos minutos, compatible con proyectos team-managed). El enlace entre commits/ramas/PRs y los issues de Jira se hace incluyendo la clave del issue (p. ej. `TFG-12`) en el mensaje de commit, nombre de rama o título de PR. La integración es de un solo sentido (GitHub → Jira) — no supone un problema porque GitHub no se usará para gestión de tareas en paralelo.

**Estado de la puesta en marcha — hecho:** proyecto Jira creado, tipo Scrum, clave `TFG`. App "GitHub for Jira" instalada y conectada al repositorio (confirmado en la práctica: las reglas de automatización descritas más abajo, que dependen de esa conexión, ya están funcionando).

**Workflow — estados decididos:** Por hacer, En curso, En revisión, Bloqueada, Finalizado.

Las transiciones del workflow se han nombrado explícitamente (Project settings → Work types → Edit workflow → botón "Transition"), en minúscula y con guion medio, para poder invocarlas por Smart Commit. Los nombres son: `start`, `review`, `done`, `reject`, `reopen`.

**Doble mecanismo de cambio de estado — decidido mantener ambos en paralelo:**
- **Automatización (Jira Automation):** reglas basadas en eventos de GitHub, configuradas para: creación de rama con la clave del issue en el nombre → En curso; creación de PR → En revisión; PR fusionada → Finalizado. *Decidido, por ahora:* la regla de PR fusionada **no** filtra por rama destino (`{{pullRequest.destinationBranch.name}}`) — se consideró la condición pero se pospuso conscientemente por no ser necesaria todavía; queda abierta para añadirla si se considera oportuno más adelante.
- **Smart Commits:** se mantienen activos igualmente, como mecanismo manual complementario (vía `#comment`, `#time`, `#<transición>` en el mensaje de commit), pese a solaparse en parte con lo que ya cubre la automatización.
- **Nota de riesgo, no una decisión:** ambos mecanismos son independientes entre sí y no se coordinan — no hay una "prioridad" definida por Jira entre ellos. Si en algún momento un Smart Commit y una regla de automatización llegan a apuntar a transiciones distintas sobre el mismo evento, pueden ejecutarse ambas sin arbitraje, con el resultado dependiendo del orden de ejecución. Con las reglas actuales el riesgo práctico es bajo porque automatización y Smart Commits no compiten por el mismo evento (rama/PR vs. commit), pero conviene vigilarlo si se añaden más reglas.
- Las transiciones `block`/`unblock` no están automatizadas ni tienen Smart Commit asociado — uso manual desde el tablero, por ser eventos que no coinciden de forma natural con un commit ni con una acción de GitHub.

**Pendiente, no confirmado en este documento:** que los requisitos previos de los Smart Commits (email de git público y coincidente con la cuenta de Jira, Time tracking activado, permisos de transición) estén realmente cumplidos — no se ha confirmado explícitamente en la conversación, solo se ha dejado como checklist.

**Plan de salida, condicional, no descartado:** si el uso de Jira resulta excesivo para el alcance de un proyecto de una sola persona, se contempla migrar a GitHub Projects. *Aviso:* no existe ruta de exportación/migración automática de Jira a GitHub Projects — el traspaso del backlog pendiente sería manual.

**Azure DevOps Boards: descartado.** *Razón:* ecosistema más pesado que las otras dos opciones para un proyecto de una sola persona, sin sinergia adicional si no se usa el resto del ecosistema Azure.

## Stack

- **Backend:** Java 21 + Spring Boot 4.1.x (actualizado desde 3.4.x: en el momento de crear el proyecto, la serie 3.4 ya había quedado fuera de las opciones activas de Initializr — solo 4.0 y 4.1 tenían soporte activo. El salto de versión mayor no afecta a las decisiones ya tomadas sobre Bean Validation, Spring Data JPA, etc.).
- **Frontend:** React con TypeScript (decidido explícitamente; antes el documento solo decía "React" sin especificar JS/TS).
- **BBDD:** PostgreSQL
- **ORM:** Spring Data JPA + Hibernate
- **Seguridad:** Spring Security con JWT. Sin librería de terceros para generar/validar el JWT — se usan las piezas ya incluidas en Spring Security 7 (`spring-boot-starter-oauth2-resource-server` para validar tokens entrantes + `spring-security-oauth2-jose` para emitirlos), en vez de una librería externa. `jjwt` queda anotado como alternativa considerada y no elegida, por si se reconsidera más adelante. Registro de usuarios: cuentas propias inicialmente; OAuth por valorar.
- **Comunicación frontend-backend:** REST
- **Frontend — toolchain de build: Vite.** Alternativas descartadas: Create React App (deprecado por el propio equipo de React, ya no es la vía recomendada ni mantenida activamente) y Next.js (orientado a SSR/rutas de servidor; obligaría a decidir modo de renderizado y dónde desplegar ese runtime Node, sin que exista ningún requisito de SEO o renderizado en servidor en el alcance del proyecto). Vite genera una SPA como build estático, coherente con el modelo de despliegue ya decidido (backend y frontend como servicios independientes, sin acoplar el frontend a un runtime Node en producción).
- **Frontend — Node.js: versión LTS activa** (no la última en sentido estricto). *Razón:* la versión más reciente de Node en cada momento suele ser una rama "Current" sin garantías de estabilidad para uso continuado, y además Corepack (usado para gestionar pnpm) deja de venir incluida por defecto a partir de Node 25.
- **Frontend — gestor de paquetes:** pnpm, instalado vía Corepack (`corepack enable` + `corepack prepare pnpm@latest --activate`), en vez de una instalación global aparte.
- **Frontend — hardening de supply-chain en pnpm:** se retrasa la instalación de versiones de paquete recién publicadas y se bloquean los scripts de instalación (`postinstall` y similares) de dependencias no revisadas explícitamente. *Razón:* mitiga paquetes npm comprometidos que se explotan en la ventana corta tras su publicación, y la ejecución automática de scripts maliciosos al instalar — un riesgo mayor aquí al no haber revisión de código de terceros en un TFG en solitario.
- **Frontend — Linter: ESLint.** Se valoró Oxlint (Rust, del mismo equipo que mantiene Vite, órdenes de magnitud más rápido) pero se descarta por ahora: la cobertura de reglas TypeScript *type-aware* (las que detectan errores reales de tipos, no solo de estilo) sigue siendo más madura en ESLint/`typescript-eslint`, y la ganancia de velocidad de Oxlint no se justifica a la escala de este proyecto.
- **Frontend — Formateador: Prettier — sin decidir.** ESLint no formatea código, solo detecta problemas; si se quiere formato automático consistente haría falta añadir Prettier aparte, con `eslint-config-prettier` para que no choquen reglas de estilo entre ambas herramientas.
- **Frontend — Enrutado: React Router**, en su *data mode* (`createBrowserRouter` + `RouterProvider`, no el modo declarativo con `<Routes>`/`<Route>`), sin su modo SSR "framework" — no aplica, ver descarte de Next.js más arriba. Alternativa valorada: TanStack Router (mejor *type-safety* en parámetros de ruta, rutas basadas en ficheros) pero descartada por ahora por ser una librería más joven y con un ecosistema más pequeño, un riesgo mayor para un TFG en solitario con plazo que la ganancia de type-safety no compensa. Vive en `app/router.tsx`; `app/App.tsx` es el componente raíz que monta `RouterProvider`.
  - **Corolario de estar en modo *data*: la carga del recurso principal de una ruta usa su `loader`, no `useEffect` + estado local.** Aplicado en la ficha de cómic (`comicDetailLoader` en `ComicDetailPage.tsx`, consumido con `useLoaderData()`; errores del `loader` los captura el `errorElement` de la ruta). *Razón:* es precisamente la capacidad que justifica haber elegido el modo *data* frente al declarativo — no usarla dejaría esa elección sin efecto práctico. No aplica a la página de búsqueda del catálogo: ahí los parámetros (texto, filtros, página) cambian repetidamente dentro de la misma ruta ya montada, un caso de estado reactivo intra-ruta para el que `useEffect` sigue siendo el patrón correcto, no una carga inicial única por navegación.
- **Frontend — Cliente HTTP: wrapper propio sobre `fetch`, no Axios.** *Razón:* Axios se había decidido inicialmente por sus interceptors, que encajan directamente con el flujo de refresco de JWT (401 → refrescar → reintentar la petición original). Pero como todas las llamadas ya iban a pasar por un módulo propio en `common/api/` (según la arquitectura ya decidida en `services/`), ese mismo módulo puede implementar el chequeo de estado, la cabecera JWT y el reintento tras refresco sin depender de una librería — Axios no aporta nada que ese wrapper no cubra a este alcance, y evita una dependencia de runtime. Contrapartida asumida: a diferencia de una instancia de Axios (donde el interceptor se aplica automáticamente a cualquier llamada hecha a través de ella), la disciplina de pasar siempre por el wrapper y no por `fetch` directo no está impuesta estructuralmente, solo por convención.
  - **El reintento tras refresco (401 → `/auth/refresh` → reintentar) solo se dispara si el 401 llega sin cuerpo `ProblemDetail`.** *Razón, detectada en la práctica:* un login con credenciales incorrectas también devuelve 401, pero es un fallo de negocio (`InvalidCredentialsException`, con cuerpo `application/problem+json`), no un token caducado. Tratarlo igual que un 401 del propio filtro de seguridad (que nunca lleva cuerpo, al ocurrir antes de llegar al controlador) hacía que el wrapper intentara refrescar con una cookie de refresco inexistente o inválida, y el error que llegaba a la pantalla de login era el del intento de refresco, no el de credenciales incorrectas. Alternativa descartada: distinguir por si la petición llevaba ya un access token adjunto — rompía la recuperación de sesión tras F5, que depende precisamente de un 401 sin token todavía disparando el refresco.
- **Frontend — i18n: `react-i18next`.** Alternativas valoradas: FormatJS/`react-intl` (formato ICU más riguroso para plurales/fechas, pero API más pesada e innecesaria para el alcance de dos idiomas de este TFG) y una solución casera con Context + JSON (cero dependencias, coherente con el criterio ya aplicado al cliente HTTP, pero sin soporte de interpolación/plurales ni documentación de comunidad). Se opta por `react-i18next` por ser el estándar de facto en React, con la mayor cobertura de casos ya resueltos sin coste de mantenimiento propio. Vive en `common/i18n/` (inicialización + diccionarios por idioma en `locales/<lang>/translation.json`); idioma activo y de fallback: inglés.
- **Frontend — Componentes UI: shadcn/ui, sobre Tailwind CSS v4 y Base UI** (no Radix) como librería de primitivas headless — decidido para acelerar el desarrollo de las pantallas de catálogo sin renunciar a poseer el código fuente de cada componente: a diferencia de una librería empaquetada (MUI, Chakra), shadcn copia el componente al repo vía su CLI, quedando editable directamente como código propio. Primitivas y tema (color base, radios, tipografía) fijados a través de un preset propio generado en el theme builder de shadcn, no reconstruible a mano. `components.json` reconfigurado tras el `init` para que sus alias (`components`, `ui`, `lib`, `hooks`) apunten a `common/` en vez de a las rutas por defecto (`src/components`, `src/lib`), coherente con dónde ya vive el resto de UI compartida. Icon set: `lucide-react` (el que trae shadcn por defecto).
  - **Bug de la CLI (v4.16.1) al resolver el alias `@/*`:** la CLI solo lee `compilerOptions.paths` del `tsconfig.json` de la raíz, sin seguir sus `references` hacia `tsconfig.app.json` (donde ya vivía el alias `@/*` real del proyecto) — sin ese bloque también en el `tsconfig.json` raíz, escribía los ficheros generados bajo una carpeta literal `./@/...` en vez de resolver a `src/`. Fix: duplicar el mismo `paths` en el `tsconfig.json` raíz, además de (no en sustitución de) `tsconfig.app.json`. Se descartó `baseUrl` como causa/solución: está deprecado desde TypeScript 6 y no tuvo ningún efecto al probarlo antes de encontrar la causa real.
  - **`react-refresh/only-export-components` bajado a warning** en `eslint.config.js`, en vez de desactivarlo: los componentes que genera shadcn exportan junto al componente una constante `cva()` (variantes de estilo) en el mismo fichero — patrón fijo de la herramienta en cada componente que instale, no evitable sin bifurcar cada uno en dos ficheros. Bajar la severidad conserva la señal de la regla para código propio sin bloquear el lint en `common/components/ui/`.
- **Frontend — Estado de búsqueda del catálogo: vive en la URL, no en estado de componente.** *Razón:* hace la búsqueda enlazable, compartible y compatible con atrás/adelante del navegador sin coste añadido — React Router (ya en modo *data*) trae `useSearchParams` de serie.
- **Frontend — Testing: se prioriza la lógica pura y estable (algoritmos, hooks) antes que tests de componente, mientras la forma de una pantalla siga en desarrollo activo.** *Razón:* un test de componente escrito contra una interfaz que todavía cambia de forma se reescribe con cada cambio; el coste de mantenerlo no se justifica hasta que esa forma se asiente.
- **Validación de entrada (backend): Bean Validation** (`spring-boot-starter-validation`), usada en los DTOs de `rest/dto/` (no en las entidades de dominio), coherente con la separación DTO/entidad ya decidida. Cubre la validación de datos aportados por el usuario (enlaces y metadatos de fuentes de lectura, entre otros).
- **Lombok: descartado.** Motivo: interfiere con el autocompletado/depuración en algunos entornos y genera código no visible en el fuente (riesgo señalado especialmente en `equals`/`hashCode` autogenerados sobre entidades JPA con relaciones lazy). Getters/setters/constructores se escriben a mano.
- **Mapeo entidad ↔ DTO: manual, sin MapStruct.** Se considera y se descarta explícitamente: el mapeo manual no se percibe como suficientemente costoso para justificar la dependencia adicional.
- **Backend — Actuator: decidido no añadirlo por ahora.** Sin un entorno de despliegue definido (ver Modelo de despliegue), no hay nada que consuma sus endpoints de salud/métricas; es una dependencia de una línea el día que haga falta.

### Decisiones técnicas relevantes

**Virtual threads (Java 21):** se habilitan con `spring.threads.virtual.enabled=true` en Spring Boot 3.2+. Los adaptadores de metadatos realizan llamadas I/O bloqueantes a APIs externas (AniList, MangaDex…); con virtual threads, la JVM aparca esos threads mientras esperan respuesta en lugar de mantener threads del SO bloqueados. Esto justifica explícitamente el uso de Java 21.

**Adaptadores de metadatos asíncronos: decidido.** Las llamadas a múltiples fuentes externas se lanzan en paralelo para combinar los resultados según prioridad configurable. Implementación: `Executors.newVirtualThreadPerTaskExecutor()` — cada adaptador se ejecuta en su propio virtual thread con una llamada bloqueante normal (`provider.fetch(...)`), y el orquestador espera a todos con `executor.invokeAll(...)`.

*Razón:*
- Es el estilo idiomático para E/S bloqueante en paralelo según el propio diseño de Project Loom/Java 21: el valor de los virtual threads es escribir código bloqueante normal sin necesidad de encadenar `CompletableFuture`, dejando que la JVM aparque el virtual thread mientras espera la respuesta de red.
- Mantiene el puerto `ComicMetadataProvider` libre de detalles de infraestructura: `fetch()` devuelve un tipo de dominio normal (`Optional<MetadataResult>`), no `CompletableFuture<...>`. La paralelización queda encapsulada en el orquestador (`MetadataAggregatorService`, en `domain/service/`), sin filtrarse al contrato del puerto — más coherente con Arquitectura Hexagonal que la alternativa con `@Async`.
- Evita el problema de self-invocation de los proxies `@Async` (no intercepta llamadas dentro de la misma clase) y la configuración adicional de executor que esa vía exige.

*Verificado, no asumido:* las llamadas a distintas fuentes sí se solapan en tiempo real (no es concurrencia aparente) — al bloquear en E/S de red, el virtual thread se desmonta de su carrier thread, liberándolo para otro adaptador. Confirmado contra documentación de Project Loom y los JEP correspondientes, no asumido por analogía con otros modelos de concurrencia.

**Riesgo a vigilar — pinning en Java 21:** un virtual thread bloqueado dentro de un `synchronized` no se desmonta de su carrier ("pinning"), reduciendo la concurrencia real. Corregido en JDK 24 (JEP 491), **no en Java 21** (la versión fijada en este TFG). No afecta a una llamada bloqueante normal sin `synchronized` alrededor (caso actual de los adaptadores), pero es relevante para la decisión pendiente de tecnología de caché (ver "Caché de respuestas de adaptadores externos" más abajo): si esa caché se implementa con `ConcurrentHashMap.computeIfAbsent(...)` y la llamada bloqueante ocurre dentro de esa función, el `synchronized` interno de `ConcurrentHashMap` sobre la clave pinearía el virtual thread en Java 21. A la escala de tráfico de este TFG (2-4 llamadas en paralelo por petición, no miles de usuarios concurrentes) el impacto sería mínimo incluso si ocurriera, pero queda anotado para tenerlo en cuenta al decidir esa tecnología.

**Alternativa descartada — `@Async` + `CompletableFuture`:** enfoque clásico de Spring, ampliamente documentado, pero descartado por las razones de arriba (el puerto tendría que devolver `CompletableFuture<...>`, filtrando un detalle de infraestructura al dominio) y porque requiere configurar explícitamente el executor para que `@Async` use virtual threads — de lo contrario no los adopta automáticamente pese a tener `spring.threads.virtual.enabled=true`.

**Caché de respuestas de adaptadores externos:** se cachean las respuestas de todos los adaptadores externos (metadatos y fuentes de lectura) para evitar llamadas redundantes a servicios de terceros. La motivación difiere según el tipo:

- *Adaptadores de metadatos:* los datos (título, autor, género…) cambian con poca frecuencia. Las APIs externas tienen rate limits y la consulta implica llamadas en paralelo a varias fuentes; repetir esa operación para cada petición no tiene sentido.
- *Adaptadores de fuentes de lectura:* los datos (capítulos disponibles, estado…) cambian con más frecuencia. La motivación aquí es principalmente evitar llamadas repetidas al mismo sitio externo en períodos cortos de tiempo, algo esperable con obras populares. Esto introduce un tradeoff inherente: los datos cacheados pueden estar desactualizados. Esta diferencia deberá tenerse en cuenta al definir la estrategia de invalidación.

**Estrategia de invalidación — adaptadores de metadatos: decidido.** TTL pasivo: cada entrada cacheada guarda el momento de la última consulta; al pedir ese cómic, si ha pasado más tiempo que el TTL configurado desde esa última consulta, se repite la llamada a los adaptadores antes de servir la respuesta; si no, se sirve el valor cacheado. No requiere infraestructura adicional (sin job programado). *Razón:* coherente con que estos datos cambian con poca frecuencia (ya establecido arriba) y con la estrategia "on-demand inicialmente" ya prevista en Adaptadores de metadatos — es su implementación concreta, no una decisión nueva independiente. **Pendiente:** el valor concreto del TTL no está fijado.

**Pendiente — rate limiting en adaptadores de metadatos:** sin throttling ni retry todavía (p. ej. `TenraiComicMetadataProvider`, límite público documentado de 3 req/s). Previsiblemente necesario cuando exista el orquestador de llamadas en paralelo (ver "Adaptadores de metadatos asíncronos" más abajo); candidata: `RateLimiter` de Resilience4j.

**Límite de paginación de Tenrai/MyAnimeList: decidido recortar `existMoreItems`/`totalItems` en vez de propagar el dato crudo de la API.** MyAnimeList tope la búsqueda en la página 1000, más allá de la cual las respuestas dejan de ser fiables aunque la propia API siga afirmando que hay más resultados. *Razón:* sin este recorte, un cliente podría intentar pedir una página inalcanzable confiando en la metadata de paginación que el adaptador expone.

*Mejora futura planteada, no decidida:* complementar el TTL pasivo con un batch periódico dirigido (no a todo el catálogo, para no multiplicar consumo de rate limit por cómic) que refresque proactivamente los cómics con más tráfico o marcados como "en emisión", reduciendo la latencia que sufre el primer usuario que pide un dato ya caducado. Coherente con el "batch periódico a futuro" ya previsto en Adaptadores de metadatos.

**Estrategia de invalidación — adaptadores de fuentes de lectura:** sigue sin decidir. La motivación es distinta (evitar llamadas repetidas en períodos cortos, no datos estables — ver arriba), por lo que el TTL pasivo decidido para metadatos no se traslada automáticamente a este caso.

La tecnología concreta de caché (almacén, etc.) no está decidida. Implementación diferida según disponibilidad de tiempo.

**Gestión de esquema de BBDD: `schema.sql` / `data.sql`, versionados en el repositorio, con `spring.jpa.hibernate.ddl-auto=none`** (o `validate`) para evitar que Hibernate genere o altere el esquema por su cuenta. Alternativas consideradas y descartadas: Flyway y Liquibase — ambas resuelven un historial incremental de cambios de esquema (migraciones numeradas, aplicables paso a paso), pero se descartan por no considerarse necesarias para un esquema que no se prevé grande. **Trade-off aceptado explícitamente:** a diferencia de una migración incremental, `schema.sql` representa el estado actual completo del esquema, no un historial paso a paso de cómo se llegó a él; el control de versiones del repositorio permite recuperar versiones anteriores del fichero, pero no aplicar cambios incrementales sobre una BBDD ya poblada sin intervención manual. **Aviso operativo comprobado:** con PostgreSQL como BBDD externa (no embebida), Spring Boot por defecto **no ejecuta `schema.sql`/`data.sql` salvo que se declare explícitamente `spring.sql.init.mode=always`** — sin esa propiedad, los scripts no se ejecutan nunca. Pendiente de revisar ese valor cuando exista un modelo de despliegue real: no debería quedarse en `always` fuera de desarrollo, porque reejecutaría los scripts en cada reinicio.

**ORM:** `FetchType.LAZY` por defecto en todas las relaciones. Las fuentes de lectura se cargan únicamente al consultar el detalle de un cómic concreto, no en el catálogo general, por lo que el problema N+1 no aplica en los casos relevantes.

**Testing de integración — Testcontainers, decidido.** Sustituye a una decisión anterior de este documento ("Testcontainers descartado por ahora"). *Motivo del cambio:* la integración `@ServiceConnection` (disponible desde Spring Boot 3.1, vigente en 4.x) elimina la fricción que originalmente motivó posponerlo — gestiona automáticamente el ciclo de vida de un contenedor PostgreSQL por clase de test, sin necesitar una segunda base de datos gestionada a mano ni coordinar un contenedor externo en CI. La clase de test por defecto generada por Initializr (`ComicTrackerApplicationTests`) está pensada para seguir este mismo patrón, pero **todavía no lo hace**: hoy depende de tener `backend/.env` configurado y una Postgres real alcanzable, en vez de un `PostgreSQLContainer` con `@ServiceConnection`. Pendiente de corregir.

**Cobertura backend — JaCoCo.** El goal `report` está atado a la fase `verify` del ciclo de vida de Maven (no a `test`). Decisión deliberada, no un descuido: si en el futuro se separan tests unitarios (Surefire, fase `test`) de tests de integración (Failsafe, fases `integration-test`/`verify`), el informe seguiría generándose correctamente después de ambos sin tener que revisar esta configuración — atarlo a `verify` no tiene coste hoy y evita un ajuste futuro.

**Análisis de calidad — SonarQube Cloud** (nombre de marca correcto; en versiones anteriores de este documento se refería como "SonarCloud"). La integración usada es distinta según el stack:
- **Backend (Maven):** se usa el plugin oficial `sonar-maven-plugin`, invocado en la misma llamada de Maven que hace el build, dentro del propio workflow de CI (ver CI/CD más abajo) — no una action de GitHub. Los identificadores de proyecto (`sonar.projectKey`, `sonar.organization`) se declaran como `<properties>` en el propio `pom.xml`.
- **Frontend (TypeScript):** al no existir un integrador de Maven/Gradle equivalente para npm/pnpm, sí se usa la action oficial de GitHub para SonarQube Cloud. Los identificadores de proyecto se declaran en `sonar-project.properties`, en la raíz de `frontend/`.  El analizador de JS/TS de Sonar interpreta JSX/TSX de forma nativa e incluye reglas propias orientadas a React, sin necesidad de plugin adicional; toma la cobertura del reporte LCOV que genera Vitest (`@vitest/coverage-v8`) con `--coverage`.
- Se opta por **dos proyectos Sonar independientes** (uno por app) en vez de un único proyecto multi-módulo, por simplicidad a esta escala. Misma organización para ambos, y ambos proyectos comparten el mismo token de autenticación (`SONAR_TOKEN`) frente a la API de SonarQube Cloud. `SONAR_TOKEN` ya está dado de alta como secret del repositorio en GitHub Actions.

## Herramientas de desarrollo y build

### Build

- **Backend:** Maven.
- **Frontend:** pnpm, gestionado de forma nativa (no envuelto dentro de Maven).

**Decisión descartada — Maven unificando backend y frontend:** se valoró usar `frontend-maven-plugin` para que un único proyecto Maven gestionase también el build del frontend (con el resultado de `pnpm build` empaquetado dentro del JAR de Spring Boot). Esto se descartó al decidir el modelo de despliegue (ver más abajo): si backend y frontend son servicios independientes, no tiene sentido acoplar sus ciclos de build en una sola herramienta. Maven gestiona únicamente el backend.

### Modelo de despliegue (arquitectura)

Backend y frontend se ejecutan y despliegan como **servicios independientes** (no como un JAR único que sirva ambos).

### Estructura de repositorio

**Monorepo**, con `/backend` y `/frontend` como carpetas separadas dentro del mismo repositorio. Se prefiere frente a repositorios separados por la menor sobrecarga de gestión a la escala de un TFG desarrollado por una sola persona.

Estructura de carpetas de la raíz del repositorio (la estructura interna de `backend/` y `frontend/` se detalla en las dos secciones siguientes):

```
/ (raíz del repo)
├── backend/                    # Proyecto Maven (Spring Boot)
│   ├── pom.xml
│   └── src/...                 # estructura interna: ver "Estructura interna del backend"
│
├── frontend/                   # Proyecto pnpm (React)
│   ├── package.json
│   ├── pnpm-lock.yaml
│   └── src/...                 # estructura interna: ver "Estructura interna del frontend"
│
├── docs/                       # Documentación del TFG — contenido exacto: pendiente
│
├── docker-compose.yml          # Solo PostgreSQL; especificaciones ya decididas — ver "Orquestación en desarrollo"
│
├── .github/
│   └── workflows/
│       └── ci.yml              # Workflow único, jobs "backend" y "frontend" — ver "CI/CD"
│
├── .gitignore                  # Único en la raíz, cubre target/ (Java) y node_modules/ (Node)
└── README.md
```

**Workflow de CI único vs separado por stack:** se valoró tener dos workflows independientes (`backend-ci.yml`, `frontend-ci.yml`) frente a uno único con dos jobs. Se decidió ir con un único `ci.yml` con jobs `backend` y `frontend`, por ser la opción más simple de gestionar a la escala de un TFG de una sola persona (un solo fichero, sin necesidad de mantener dos en paralelo). Si el pipeline crece en complejidad, separar los jobs en workflows independientes es un cambio sencillo de aplicar más adelante.

**`docs/`:** carpeta para la documentación del TFG, separada del código. El contenido exacto que irá dentro (memoria, diagramas, anteproyecto, este propio documento…) queda pendiente de decidir.

### Estructura interna del backend

**Patrón: Arquitectura Hexagonal (Ports & Adapters)**, adoptada y nombrada explícitamente. *Razón:* los adaptadores de metadatos y de fuentes de lectura no son "acceso a datos" en sentido estricto (no son una base de datos, y hay varios tipos distintos); el patrón Hexagonal resuelve esto tratando la persistencia como un adaptador más entre varios (hacia BD, hacia APIs externas, hacia sitios web de terceros), todos al mismo nivel, cada uno implementando un puerto (interfaz) definido por el dominio. Esto son adaptadores *driven*; el patrón también reconoce adaptadores *driving* (traducen una llamada externa en una llamada a los propios métodos del dominio, sin necesitar una interfaz de entrada explícita) — caso de `rest/`, ver más abajo.

```
backend/src/main/java/.../
├── domain/
│   ├── entities/          Comic, ComicMetadataSource, ComicMetadataEntry, ComicReadingSource,
│                           ComicReadingEntry (nombres definitivos — ver "Convención de
│                           nombres de entidades y adaptadores") (crecerá: p. ej.
│                           Valoracion para moderación)
│   ├── port/               puertos + sus tipos de contrato, sin implementaciones: metadata/, source/
│   ├── common/             tipos de dominio genéricos reutilizables entre puertos
│   ├── exceptions/         excepciones de dominio, sin conocimiento de HTTP
│   └── service/             lógica de negocio: combinar adaptadores de metadatos por
│                           prioridad, deduplicación, cálculo de progreso
│
└── adapter/
    ├── persistence/         adaptador hacia BD: repositorios JPA
    ├── metadata/             implementaciones de ComicMetadataProvider (AniList, MangaDex...)
    ├── source/                implementaciones de ComicReadingProvider, detectadas por dominio
    └── rest/                  adaptador driving: controller/, dto/, mapper/, exception/
                             (GlobalExceptionHandler), security/
```

Razones puntuales:
- **`domain/port/` como subpaquete dedicado a puertos, en vez de una capa `application/` intermedia:** se valoró introducir una tercera capa entre `domain/` y `adapter/` (estilo Clean Architecture) tras detectar que `ComicMetadataProvider` había acabado viviendo dentro de `adapter/metadata/`, junto a su propia implementación — contradiciendo la premisa de que el dominio define el puerto. Se descarta esa capa adicional: añade una frontera más sin una necesidad demostrada a la escala de este TFG. La solución adoptada es más modesta: aislar los puertos en su propio subpaquete dentro de `domain/`, separados de `entities/`, sin crear una capa nueva.
- **`rest/` (antes `web/`, separado de `adapter/`) sí va dentro de `adapter/`:** la razón anterior para mantenerlo fuera ("no hay puerto de dominio que implemente, luego no hay simetría con `persistence/metadata/source`") asumía que "puerto" solo aplica en el sentido *driven* — pero no es la condición que define un adaptador *driving*: los controllers REST adaptan igual, solo que en la dirección contraria de dependencia, con o sin interfaz de entrada explícita. Se renombra a `rest/` (no `web/`) por precisión: es lo que es.
- **DTOs y mappers, dentro de `adapter/rest/`** (no en un paquete aparte reutilizable): exclusivos de esta vía de entrada por defecto, hasta que se demuestre una necesidad real de reutilizarlos desde otra. Separados de las entidades JPA por `FetchType.LAZY` (ya decidido) — serializarlas directamente como respuesta REST puede dar problemas (proxies, `LazyInitializationException`, ciclos).
- **`security/` dentro de `adapter/rest/`**, en lugar de un paquete propio al mismo nivel que `domain/`/`adapter/`: justificado porque, en el alcance actual del TFG, el 100% de lo que protege el JWT son los endpoints REST — no hay otro punto de entrada. **Parcialmente resuelto** (ver "Usuario: modelo de datos y registro"): lo que sí es específico de REST (`SecurityConfig`, futuro `JwtAuthenticationFilter`) se queda aquí; lo que no lo es (hash de contraseña, previsiblemente JWT) se modela aparte, como puerto de dominio + adaptador *driven*, hermano de `persistence/`.
- **`exceptions/` dentro de `domain/`:** las excepciones de negocio no deben conocer HTTP; su traducción a códigos de estado se centraliza aparte, en `adapter/rest/exception/`. **Pendiente:** aún sin implementar.

### Estructura interna del frontend

```
frontend/src/
├── app/                    routing + bootstrap
├── common/
│   ├── api/                 cliente HTTP base (wrapper sobre fetch; baseURL + cabecera
│   │                        JWT + reintento tras refresco)
│   ├── components/           componentes UI genéricos compartidos entre módulos
│   └── i18n/                 inicialización de react-i18next + diccionarios por idioma
│
├── services/                 DTOs (tal cual los devuelve la API) + llamadas API, por entidad
│   ├── comic/    { types/, api/ }
│   ├── source/   { types/, api/ }
│   └── user/     { types/, api/ }
│
└── modules/                  composición de UI por funcionalidad (auth, catalog, tracking...)
```

Razones puntuales:
- **`services/` separado de `modules/`:** las entidades de negocio (Comic, ComicReadingEntry, Usuario) no corresponden 1:1 con los módulos de UI — una misma pantalla puede necesitar varias entidades a la vez (p. ej. detalle de cómic + sus fuentes), y un módulo de UI puede no mapear a una sola entidad. Mantenerlas separadas evita forzar esa correspondencia. Inspirado en el concepto de capa "entities" de Feature-Sliced Design, sin adoptar sus reglas estrictas de importación entre capas.
- **Los tipos en `services/` son DTOs, no modelos de UI transformados:** simetría intencionada con `rest/dto` del backend — ambos representan la forma del contrato HTTP, no un modelo de dominio enriquecido.
- **Sin capa de mapeo DTO → modelo de UI por ahora:** los componentes consumen los DTOs directamente. Se acepta el acoplamiento resultante (un cambio de forma en un DTO impacta a los componentes que lo usan) porque, al ser un proyecto de una sola persona controlando ambos extremos, el coste de ese acoplamiento cuando ocurra es manejable. **Revisar si:** (a) un componente necesita combinar DTOs de varias entidades a la vez, o (b) se necesita un campo derivado que no viene tal cual de la API — en cualquiera de esos casos, introducir el mapeo en ese módulo concreto, no en todos a la vez.
- **`common/` fuera de `modules/`:** deliberado, para que cualquier módulo pueda importar de `common/` sin necesidad de una regla de excepción — al no ser un módulo más, no rompe el principio de que los módulos no se importan entre sí.

### Orquestación en desarrollo

- **Docker Compose:** se usa para levantar **PostgreSQL** en local. Especificaciones ya decididas (contenido completo en el propio `docker-compose.yml` del repositorio):
  - Imagen `postgres:18-alpine`, con **ICU como proveedor de locale** (en vez del proveedor por defecto de la libc del sistema). *Razón:* Alpine usa `musl` como libc, que solo soporta de forma nativa las locales `C`/`POSIX` — insuficiente para ordenar/buscar correctamente títulos en español, algo relevante para el catálogo. ICU trae sus propios datos de locale, independientes del sistema operativo, resolviendo el problema sin renunciar al menor tamaño de imagen de Alpine.
  - Volumen montado en `/var/lib/postgresql` (no en `/var/lib/postgresql/data`, válido hasta PostgreSQL 17 pero no desde la 18, por un cambio de layout de la propia imagen oficial hacia un formato compatible con `pg_ctlcluster`).
  - Ambos parámetros (locale y layout del volumen) se fijan una sola vez, al crear el volumen — cualquier cambio posterior requiere recrearlo desde cero.
- **Backend:** se lanza nativo (`mvn spring-boot:run` o equivalente).
- **Frontend:** se lanza nativo (`pnpm dev`).
- *Evolución futura prevista (no decidida aún):* extender el `docker-compose.yml` para incluir también backend y frontend cuando el proyecto esté más maduro.

### Gestión de secrets

Ningún secreto (credenciales de BBDD, futuro secreto de firma del JWT) se hardcodea en el código ni se versiona.

- **Backend:** Spring Boot no soporta ficheros `.env` de forma nativa. Se usa la dependencia **`springboot4-dotenv`** para cargar `backend/.env` (gitignorado), con las credenciales de conexión consumidas en `application.yml` vía placeholders.
- **Docker Compose (raíz del repo):** lee su propio `.env` (gitignorado) de forma nativa, con las credenciales del contenedor Postgres. Es un fichero distinto al de `backend/.env`, con valores que hay que mantener sincronizados a mano entre ambos — no hay una única fuente de verdad automática, y no se ha considerado necesario añadir infraestructura para evitarlo a esta escala.
- **Frontend:** Vite lee `.env`/`.env.local` de forma nativa. Convención decidida: solo se exponen al código cliente variables con prefijo `VITE_`. **A día de hoy no existe `frontend/.env.local`** — no hace falta todavía, porque el frontend no necesita ninguna variable de entorno en el punto actual del desarrollo; se creará cuando exista una (p. ej. la URL del backend). La URL del backend, en cualquier caso, no se trata como secreto real.
- **CORS:** pendiente de implementar en `SecurityConfig` — necesario para que el frontend (origen distinto al del backend) pueda llamar a la API. No es una decisión de arquitectura, es un requisito de funcionamiento básico una vez ambos servicios corran de forma independiente.

### Documentación de la API

**Springdoc OpenAPI**, con **Scalar** como interfaz (`springdoc-openapi-starter-webmvc-scalar`, en vez del módulo `-webmvc-ui` de Swagger UI clásico) — mismo generador de la especificación a partir de las anotaciones del código en tiempo de ejecución, ya decidido; solo cambia qué interfaz la renderiza. *Razón:* interfaz más moderna, mantenida como módulo oficial dentro del propio proyecto springdoc-openapi, no una herramienta de terceros aparte.

### Testing

- **Backend:** stack nativo de Spring Boot — JUnit 5 + Mockito.
- **BBDD para tests de integración: Testcontainers, decidido** (revierte la decisión anterior de posponerlo — ver justificación en "Decisiones técnicas relevantes"). Usa la misma imagen y configuración de locale que en desarrollo.
- **Frontend: Vitest, revierte la decisión anterior de usar Jest.** *Razón del cambio:* la fricción que ya se anticipaba al elegir Jest (config adicional vía `ts-jest`/`babel-jest`, `moduleNameMapper` para el alias `@/*` que ya vive en `vite.config.ts`, mocks manuales para imports de CSS/assets que Jest no sabe interpretar) se confirmó en la práctica al configurarlo, sin que hubiera una razón de peso para mantener Jest en su lugar. Vitest reutiliza `vite.config.ts` tal cual (mismo alias, mismo manejo de CSS/assets que en dev/build) y su API es compatible con la de Jest (`describe`/`it`/`expect`), así que no cambia el paradigma de test. Revertido con coste cero: todavía no existía ningún fichero de test.
- **Cobertura backend:** JaCoCo (plugin Maven), cuyo informe alimenta el análisis de SonarQube Cloud.
- **Cobertura frontend:** Vitest con `@vitest/coverage-v8` y la flag `--coverage`, en formato LCOV, consumido por el análisis de SonarQube Cloud del frontend.
- **E2E (tentativo, no cerrado):** Playwright. Pendiente de confirmar cuando haya una arquitectura estable de endpoints y vistas.
- **Rendimiento/carga (tentativo, no cerrado):** k6. Pendiente de confirmar más adelante.

### CI/CD — GitHub Actions

Pipeline en un único `ci.yml` (contenido completo en el propio fichero del repositorio), con jobs `backend` y `frontend`.

**Filtrado por carpeta cambiada:** no existe un filtro `paths` a nivel de job individual (ese filtro solo aplica a todo el workflow) — se usa en su lugar un job previo (`changes`) con la action `dorny/paths-filter`, del que dependen condicionalmente los jobs `backend` y `frontend`. Corrige el planteamiento de una versión anterior de este documento, que asumía incorrectamente que era posible un filtro `paths` por job.

**Backend:** una única invocación de Maven que cubre build, tests (incluidos los de Testcontainers, que gestionan su propio contenedor Postgres sin nada adicional que preparar en el runner), cobertura (JaCoCo) y análisis de SonarQube Cloud — todo en la misma sesión de Maven, para que el análisis recoja los datos de cobertura de la ejecución que él mismo presencia, sin depender de qué sobreviva en disco entre comandos separados. Ya no hace falta declarar ningún servicio Postgres a nivel de job (a diferencia de un planteamiento anterior de este documento) — lo gestiona Testcontainers directamente.

**Frontend:** instalación con `pnpm` (usando `pnpm/action-setup` antes de `actions/setup-node`, necesario porque pnpm no viene preinstalado en los runners), lint (ESLint), build, test con cobertura (Vitest), y análisis de SonarQube Cloud vía la action oficial correspondiente.

**Calidad de código — SonarQube Cloud** (corrección de nombre de marca respecto a versiones anteriores de este documento, que decían "SonarCloud"; la action que se había anotado inicialmente, específica de SonarCloud, está deprecada en favor de una action unificada). Integración distinta según stack, según se detalla en "Decisiones técnicas relevantes".

**Pendiente, decisión consciente de posponer:** bloque `permissions` (lectura de contenido y de pull requests) en el job de filtrado por carpetas — recomendado por la documentación de la action usada para ese filtrado, no añadido todavía; se revisará más adelante.

**Deploy** *(por decidir)*
No hay destino de despliegue fijado (VPS propio, plataforma cloud, servidor de la UDC, etc.). Por tanto, el job de deploy del pipeline no existe todavía; se añadirá cuando se decida dónde se alojará la aplicación. Estará condicionado a la rama `main` y dependiente de que los jobs de build/test/calidad pasen correctamente.

**Optimización disponible (no aplicada todavía):** al ser un monorepo, los jobs de backend y frontend ya solo se ejecutan cuando cambian archivos de su carpeta correspondiente (ver filtrado por carpeta cambiada, arriba) — esta optimización, planteada inicialmente como pendiente, queda resuelta con el cambio a `dorny/paths-filter`.

## Directores
- David Otero Freijeiro — david.otero.freijeiro@udc.es
- Miguel Anxo Pérez Vila — anxo.pvila@udc.es