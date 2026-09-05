# Mission Briefing Planner

Planificador de misiones operativas propias (reconocimiento, logística, escolta): define
una ruta con waypoints y tareas, un cronograma de fases y los recursos asignados, y genera
un briefing de misión buscable en un archivo histórico. Backend REST en Java/Spring Boot,
cliente de escritorio en JavaFX que embebe un módulo Swing heredado para la vista de mapa.
Incluye una simulación del movimiento del convoy sobre su ruta con aviso en vivo al pasar
por una zona de riesgo conocida, y una simulación de pérdida de escolta que redirige la
misión al punto de extracción seguro más cercano.

## Motivación

La planificación de misiones a menudo vive repartida entre hojas de cálculo, documentos
sueltos y correo — sin un modelo de datos único ni forma de buscar "¿qué misión pasó por
esta zona el mes pasado?" entre decenas de briefings antiguos. Este proyecto centraliza
ese flujo: un modelo de misión estructurado, un generador de briefing y un archivo
histórico con búsqueda de texto libre.

## Arquitectura

Proyecto Maven multi-módulo:

| Módulo | Rol |
|---|---|
| `mission-model` | Entidades de dominio compartidas (JPA): `Mission`, `Waypoint`, `MissionPhase`, `Resource`. Sin dependencia de Spring. |
| `mission-server` | API REST (Spring Boot). Persistencia estructurada en **Postgres** vía JPA/Hibernate; texto libre de briefings indexado en **ElasticSearch** para búsqueda histórica; ruta real por carretera vía el servicio público de **OSRM**. |
| `mission-client-swing-legacy` | Módulo Swing "heredado": visor de mapa (`jxmapviewer2`, tiles OpenStreetMap). Sustituye a Luciad (SDK de mapas propietario) por una librería libre equivalente. |
| `mission-client-fx` | Cliente de escritorio principal, **JavaFX**. Consume la API REST y embebe `mission-client-swing-legacy` vía `SwingNode` en vez de reescribir el visor de mapa — el mismo patrón de modernización parcial habitual en aplicaciones de escritorio de este sector. |

```
mission-client-fx  ──SwingNode──▶  mission-client-swing-legacy
       │                                     │
       └──────────────┬──────────────────────┘
                       ▼
                 mission-model
                       ▲
                       │
                mission-server ──▶ Postgres (misiones)
                       └────────▶ ElasticSearch (briefings)
```

## Cómo ejecutarlo

Requiere JDK 21. Este repo incluye `mvnw`/`mvnw.cmd` (Maven Wrapper), así que no hace
falta tener Maven instalado aparte.

1. Levantar Postgres y ElasticSearch localmente:

   ```bash
   docker compose up -d
   ```

2. Arrancar el servidor REST (crea el esquema de Postgres con Flyway automáticamente):

   ```bash
   ./mvnw -pl mission-server spring-boot:run
   ```

3. Arrancar el cliente de escritorio (en otra terminal):

   ```bash
   ./mvnw -pl mission-client-fx javafx:run
   ```

La API exige autenticación básica (usuario `admin`, contraseña `missionbriefing` por
defecto — cambiable con la variable de entorno `MISSION_API_PASSWORD` en el servidor y
`MISSION_API_USER`/`MISSION_API_PASSWORD` en el cliente). El cliente JavaFX ya manda las
credenciales solo; para llamar a la API a mano hace falta `-u admin:missionbriefing`.

## Crear, editar y borrar una misión

Todo el CRUD de misiones está disponible desde el propio cliente, no solo por API:

- **"+ Nueva misión"** abre un formulario (nombre, tipo, fecha/hora de inicio y fin en
  UTC, descripción, y filas dinámicas de waypoints/fases/escoltas con botones "+"/"✕").
  Llama a `POST /api/missions`.
- **"✏ Editar misión"** abre el mismo formulario precargado con los datos de la misión
  seleccionada. Llama a `PUT /api/missions/{id}`, que reemplaza por completo nombre,
  tipo, fechas y todos los waypoints/fases/escoltas (no hay actualización parcial).
- **"🗑 Borrar misión"** pide confirmación y llama a `DELETE /api/missions/{id}`.

En los tres casos la lista se refresca sola al terminar. También se puede hacer
directamente por API:

## Uso de la API

```bash
# Crear una mision
curl -u admin:missionbriefing -X POST http://localhost:8080/api/missions \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Patrulla costera",
        "type": "RECONNAISSANCE",
        "startTime": "2026-09-10T08:00:00Z",
        "endTime": "2026-09-10T10:00:00Z",
        "description": "Reconocimiento de la linea de costa",
        "waypoints": [
          {"sequenceOrder": 1, "latitude": 36.15, "longitude": -5.35, "taskType": "OBSERVE", "notes": "Punto norte"}
        ],
        "phases": [
          {"name": "Transito", "startOffsetMinutes": 0, "endOffsetMinutes": 30, "notes": "Salida"}
        ]
      }'

# Generar el briefing de esa mision (indexado en ElasticSearch)
curl -u admin:missionbriefing -X POST http://localhost:8080/api/missions/1/briefing

# Buscar en el archivo historico de briefings
curl -u admin:missionbriefing "http://localhost:8080/api/briefings/search?q=costera"

# Catalogo de zonas de riesgo (ilustrativas, ver "Limites eticos")
curl -u admin:missionbriefing "http://localhost:8080/api/risk-zones"

# Ruta real por carretera de una mision (OSRM), para animar el convoy sobre calles reales
curl -u admin:missionbriefing "http://localhost:8080/api/missions/1/road-route"

# Catalogo de puntos de extraccion (retirada segura)
curl -u admin:missionbriefing "http://localhost:8080/api/extraction-points"

# Punto de extraccion mas cercano a una posicion, con ruta real hasta el
curl -u admin:missionbriefing "http://localhost:8080/api/extraction-points/nearest-route?lat=36.135&lon=-5.447"

# Actualizar una mision existente (reemplazo completo)
curl -u admin:missionbriefing -X PUT http://localhost:8080/api/missions/1 -H "Content-Type: application/json" -d '{...}'

# Borrar una mision
curl -u admin:missionbriefing -X DELETE http://localhost:8080/api/missions/1
```

## Escoltas y extracción de emergencia

Una misión puede llevar escoltas asignados (`resources` al crearla, tipo `PERSONNEL_TEAM`).
En el cliente JavaFX se ven como un "monigote" azul (🚶) junto al convoy, con su
indicativo al lado; cada uno tiene un botón **"✕ Marcar perdido"** en el panel izquierdo.

Si se marca un escolta como perdido:
1. Se congela en su última posición conocida (icono atenuado + X roja) y ya no se mueve
   con el convoy.
2. El banner superior pasa a **"🚨 SEGURIDAD COMPROMETIDA"**.
3. El cliente pide al servidor el punto de extracción más cercano a la posición actual
   del convoy y la ruta real hasta él (`GET /api/extraction-points/nearest-route`).
4. La animación se desvía hacia ese punto en vez de continuar la ruta original.

Si nadie se marca como perdido, la misión simplemente llega a su destino previsto — dos
misiones de ejemplo ("Escolta sin incidentes" / "Escolta con incidente") muestran los dos
desenlaces con los mismos dos escoltas asignados.

## Exportar el briefing a PDF

Botón **"📄 Exportar briefing (PDF)"**: genera un documento PDF real (con
[Apache PDFBox](https://pdfbox.apache.org/), sin fuentes externas que empaquetar) con
portada (nombre, tipo, estado, fechas, descripción), una **captura del mapa tal como se
ve en ese momento** (waypoints, zonas de riesgo, convoy/escoltas si la animación está en
marcha — `LegacyMapPanel.snapshot()`), cronograma, ruta y escoltas asignados. Un
`FileChooser` nativo pregunta dónde guardarlo. Pensado para ser un documento imprimible
de verdad, no solo una vista en pantalla.

## Interfaz

El cliente JavaFX usa una hoja de estilos propia (`mission-briefing.css`, tema oscuro
tipo "consola de operaciones") en vez del `Modena` por defecto: el panel izquierdo se
organiza en secciones (misiones, escoltas, simulación de convoy) con jerarquía visual
clara, y el banner de alerta cambia de color según haya o no una incidencia activa.

## Simulación de convoy y zonas de riesgo

El cliente JavaFX pinta un catálogo de zonas de riesgo (círculos coloreados por nivel:
bajo/medio/alto) sobre el mapa, y puede animar el movimiento del convoy de la misión
seleccionada en tiempo simulado (comprimido a ~12 segundos, independientemente de la
duración real de la misión). Si la posición animada entra en una zona de riesgo, se
enciende un aviso en la parte superior de la ventana con el motivo; al salir, se apaga.
Botones "Iniciar / Pausar / Reiniciar" en el panel izquierdo.

**La animación sigue calles reales, no líneas rectas entre waypoints**: al pulsar
"Iniciar", el cliente pide a `mission-server` la ruta real por carretera entre los
waypoints de la misión (`GET /api/missions/{id}/road-route`), que a su vez la calcula
llamando al servicio público de [OSRM](https://project-osrm.org/) (Open Source Routing
Machine) tramo a tramo y concatenando el resultado. La velocidad visual del convoy es
constante independientemente de cuántos puntos tenga cada tramo de la ruta (se reparte
por distancia recorrida, no por número de puntos — ver `RouteAnimationMath`). Si el
servicio de rutas no responde (sin red, límite de uso del demo público), el cliente cae
automáticamente a animar en línea recta entre waypoints, sin romperse.

Un GPS real encajaría en el mismo punto exacto donde hoy entra la posición simulada:
`ConvoyMarkerPainter`/la detección de zona de riesgo trabajan sobre "la posición actual"
sin saber ni importarles si viene de una interpolación o de un receptor real.

## Calidad

- JUnit 5 + AssertJ en los cuatro módulos; MockMvc + H2 en memoria para los endpoints REST
  (sin necesitar Postgres/ElasticSearch levantados para correr los tests).
- Cobertura con JaCoCo (`mvn test`, informe en `target/site/jacoco` de cada módulo).
- CI en GitHub Actions (`mvn -B test`) en cada push/PR a `main`.
- Esquema de Postgres versionado con **Flyway** (`mission-server/src/main/resources/db/migration`)
  en vez de `hibernate.ddl-auto=update`: Hibernate arranca en modo `validate` (falla rápido si
  una entidad y la tabla real no coinciden) y cada cambio de esquema queda en un script
  numerado y revisable, no generado implícitamente.
- API protegida con **autenticación básica** (Spring Security, ver "Cómo ejecutarlo"): sin
  credenciales, cualquier endpoint responde `401`. El cliente JavaFX las manda solas.

## Límites éticos

Esta herramienta **no** hace nada de lo siguiente, a propósito:

- No controla ningún activo real (dron, vehículo, embarcación) ni se conecta a hardware.
- No es un sistema de mando y control (C2) en tiempo real — es planificación y
  documentación offline.
- No incluye misiones de ataque ni cálculo de daño: los tipos de misión son
  reconocimiento, logística y escolta.
- Las **zonas de riesgo son un catálogo de ejemplo, hardcoded** (`RiskZoneCatalog`), no
  inteligencia real ni datos de ningún proveedor — sirven para demostrar el concepto de
  "avisar a mi propia ruta", no para identificar amenazas reales.
- La animación del convoy es una **simulación de tiempo comprimido** sobre datos propios
  (la ruta que tú mismo planificaste), pensada para *avisar a la misión propia*, no para
  planear ni dirigir ninguna acción contra otra parte.
- La ruta real por carretera usa el **servicio demo público de OSRM**, pensado para
  pruebas/demos, no para producción (sin SLA, con límite de uso, y las coordenadas viajan
  a un tercero). Un despliegue real usaría una instancia propia de OSRM o un proveedor de
  pago — el punto de extensión (`OsrmRouteClient`) ya está aislado para ese cambio.
- Los **puntos de extracción son un catálogo de ejemplo** (`ExtractionPointCatalog`),
  igual que las zonas de riesgo. La función de "escolta perdido → redirigir a extracción"
  es una **herramienta de retirada a un lugar seguro para proteger a la propia misión**,
  no de ataque ni de persecución de nada — el punto de extracción es a donde se retira
  uno mismo, no un objetivo.

## Posibles extensiones

- i18n (ES/EN) en el cliente de escritorio, como en el resto del portafolio.
- Tests dedicados de la capa JavaFX (hoy la UI se prueba manualmente; lo probado
  automáticamente son los módulos de servidor, modelo y el visor Swing).
