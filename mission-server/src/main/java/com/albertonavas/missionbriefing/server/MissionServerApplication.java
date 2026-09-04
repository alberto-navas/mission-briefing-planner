package com.albertonavas.missionbriefing.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * mission-model vive en un paquete hermano (com.albertonavas.missionbriefing.model), no
 * descendiente de este, asi que el escaneo de entidades JPA se declara explicitamente.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.albertonavas.missionbriefing.model")
public class MissionServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MissionServerApplication.class, args);
    }
}
