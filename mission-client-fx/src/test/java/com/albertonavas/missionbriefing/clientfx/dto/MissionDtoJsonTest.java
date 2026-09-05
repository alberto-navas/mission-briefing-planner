package com.albertonavas.missionbriefing.clientfx.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

/** Verifica que el DTO del cliente deserializa el mismo JSON que emite mission-server. */
class MissionDtoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void deserializesServerShapedJson() throws Exception {
        String json = """
                {
                  "id": 1,
                  "name": "Patrulla costera",
                  "type": "RECONNAISSANCE",
                  "status": "DRAFT",
                  "startTime": "2026-09-10T08:00:00Z",
                  "endTime": "2026-09-10T10:00:00Z",
                  "description": "Reconocimiento de la linea de costa",
                  "waypoints": [
                    {"id": 1, "sequenceOrder": 1, "latitude": 36.15, "longitude": -5.35, "taskType": "OBSERVE", "notes": "Punto norte"}
                  ],
                  "phases": [],
                  "resources": [
                    {"id": 1, "name": "Escolta 1", "type": "PERSONNEL_TEAM", "callSign": "BRAVO-1", "available": false}
                  ]
                }
                """;

        MissionDto mission = mapper.readValue(json, MissionDto.class);

        assertThat(mission.name()).isEqualTo("Patrulla costera");
        assertThat(mission.type()).isEqualTo("RECONNAISSANCE");
        assertThat(mission.waypoints()).hasSize(1);
        assertThat(mission.waypoints().get(0).taskType()).isEqualTo("OBSERVE");
        assertThat(mission.resources()).hasSize(1);
        assertThat(mission.resources().get(0).callSign()).isEqualTo("BRAVO-1");
        assertThat(mission.toString()).isEqualTo("Patrulla costera (RECONNAISSANCE)");
    }
}
