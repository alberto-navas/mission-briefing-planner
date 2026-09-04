package com.albertonavas.missionbriefing.server.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

        mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Patrulla costera")))
                .andExpect(jsonPath("$.waypoints.length()", is(1)))
                .andExpect(jsonPath("$.waypoints[0].taskType", is("OBSERVE")))
                .andExpect(jsonPath("$.phases.length()", is(1)));

        mockMvc.perform(get("/api/missions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Patrulla costera")));
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
