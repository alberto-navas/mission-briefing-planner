package com.albertonavas.missionbriefing.server.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * La API exige autenticacion basica (ver SecurityConfig); esto anade la cabecera
 * Authorization por defecto a cada peticion de MockMvc, para no repetirla en cada test.
 * Las credenciales deben coincidir con spring.security.user.* de application.yml de test.
 */
@TestConfiguration
public class MockMvcAuthConfig {

    public static final String TEST_USER = "test-user";
    public static final String TEST_PASSWORD = "test-pass";

    @Bean
    MockMvcBuilderCustomizer defaultAuthCustomizer() {
        String credentials = Base64.getEncoder()
                .encodeToString((TEST_USER + ":" + TEST_PASSWORD).getBytes(StandardCharsets.UTF_8));
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/").header("Authorization", "Basic " + credentials));
    }
}
