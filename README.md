# Mission Briefing Planner

Planificador de misiones operativas propias (reconocimiento, logística, escolta): define
una ruta con waypoints y tareas, un cronograma de fases y los recursos asignados, y genera
un briefing de misión buscable en un archivo histórico. Backend REST en Java/Spring Boot,
cliente de escritorio en JavaFX que embebe un módulo Swing heredado para la vista de mapa.

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
| `mission-server` | API REST (Spring Boot). Persistencia estructurada en **Postgres** vía JPA/Hibernate; texto libre de briefings indexado en **ElasticSearch** para búsqueda histórica. |
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

2. Arrancar el servidor REST:

   ```bash
   ./mvnw -pl mission-server spring-boot:run
   ```

3. Arrancar el cliente de escritorio (en otra terminal):

   ```bash
   ./mvnw -pl mission-client-fx javafx:run
   ```

## Uso de la API

```bash
# Crear una mision
curl -X POST http://localhost:8080/api/missions \
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
curl -X POST http://localhost:8080/api/missions/1/briefing

# Buscar en el archivo historico de briefings
curl "http://localhost:8080/api/briefings/search?q=costera"
```

## Calidad

- JUnit 5 + AssertJ en los cuatro módulos; MockMvc + H2 en memoria para los endpoints REST
  (sin necesitar Postgres/ElasticSearch levantados para correr los tests).
- Cobertura con JaCoCo (`mvn test`, informe en `target/site/jacoco` de cada módulo).
- CI en GitHub Actions (`mvn -B test`) en cada push/PR a `main`.

## Límites éticos

Esta herramienta **no** hace nada de lo siguiente, a propósito:

- No controla ningún activo real (dron, vehículo, embarcación) ni se conecta a hardware.
- No es un sistema de mando y control (C2) en tiempo real — es planificación y
  documentación offline.
- No incluye misiones de ataque ni cálculo de daño: los tipos de misión son
  reconocimiento, logística y escolta.

## Posibles extensiones

- Formulario de creación/edición de misión en el cliente JavaFX (hoy la creación se hace
  vía API; la UI de escritorio es de solo lectura + mapa).
- Migraciones de esquema con Flyway/Liquibase en vez de `hibernate.ddl-auto=update`.
- Exportar el briefing generado a PDF.
- Autenticación en la API REST.
