package com.albertonavas.missionbriefing.server.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.albertonavas.missionbriefing.server.support.MockMvcAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class MissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndListMission() throws Exception {
        String body = """
                {
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
                }
                """;

        String createResponse = mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Patrulla costera")))
                .andExpect(jsonPath("$.waypoints.length()", is(1)))
                .andExpect(jsonPath("$.waypoints[0].taskType", is("OBSERVE")))
                .andExpect(jsonPath("$.phases.length()", is(1)))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        // Se busca por su propio id, no por posicion en la lista: los tests comparten la
        // misma base H2 y no se puede asumir que esta mision sea la unica ni la primera.
        mockMvc.perform(get("/api/missions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Patrulla costera")));
    }

    @Test
    void updateReplacesFieldsAndChildCollections() throws Exception {
        String createBody = """
                {
                  "name": "Escolta original",
                  "type": "ESCORT",
                  "startTime": "2026-09-10T08:00:00Z",
                  "endTime": "2026-09-10T10:00:00Z",
                  "waypoints": [
                    {"sequenceOrder": 1, "latitude": 36.0, "longitude": -5.0, "taskType": "TRANSIT", "notes": "viejo"}
                  ]
                }
                """;
        String createResponse = mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = """
                {
                  "name": "Escolta actualizada",
                  "type": "ESCORT",
                  "startTime": "2026-09-11T08:00:00Z",
                  "endTime": "2026-09-11T10:00:00Z",
                  "description": "Actualizada",
                  "waypoints": [
                    {"sequenceOrder": 1, "latitude": 36.2, "longitude": -5.2, "taskType": "OBSERVE", "notes": "nuevo1"},
                    {"sequenceOrder": 2, "latitude": 36.3, "longitude": -5.3, "taskType": "OBSERVE", "notes": "nuevo2"}
                  ]
                }
                """;

        mockMvc.perform(put("/api/missions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Escolta actualizada")))
                .andExpect(jsonPath("$.waypoints.length()", is(2)))
                .andExpect(jsonPath("$.waypoints[0].notes", is("nuevo1")));

        // El waypoint viejo no debe seguir presente tras el reemplazo completo.
        mockMvc.perform(get("/api/missions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waypoints.length()", is(2)))
                .andExpect(jsonPath("$.waypoints[*].notes", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("viejo"))));
    }

    @Test
    void updateOfMissingMissionReturnsNotFound() throws Exception {
        String updateBody = """
                {
                  "name": "No existe",
                  "type": "ESCORT",
                  "startTime": "2026-09-10T08:00:00Z",
                  "endTime": "2026-09-10T10:00:00Z"
                }
                """;

        mockMvc.perform(put("/api/missions/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesMission() throws Exception {
        String createBody = """
                {
                  "name": "Mision a borrar",
                  "type": "LOGISTICS",
                  "startTime": "2026-09-10T08:00:00Z",
                  "endTime": "2026-09-10T10:00:00Z"
                }
                """;
        String createResponse = mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/missions/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/missions/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOfMissingMissionReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/missions/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissionWithoutName() throws Exception {
        String body = """
                {
                  "type": "LOGISTICS",
                  "startTime": "2026-09-10T08:00:00Z",
                  "endTime": "2026-09-10T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
