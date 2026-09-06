package com.albertonavas.missionbriefing.server.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestsWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/risk-zones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongCredentials() throws Exception {
        mockMvc.perform(get("/api/risk-zones").with(httpBasic("test-user", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsCorrectCredentials() throws Exception {
        mockMvc.perform(get("/api/risk-zones").with(httpBasic("test-user", "test-pass")))
                .andExpect(status().isOk());
    }
}
