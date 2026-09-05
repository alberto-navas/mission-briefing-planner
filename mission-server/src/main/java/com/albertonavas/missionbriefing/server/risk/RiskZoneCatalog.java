package com.albertonavas.missionbriefing.server.risk;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Catalogo estatico de zonas de riesgo de ejemplo, en el area del Estrecho de Gibraltar
 * (misma zona demo que el resto del portafolio). Ilustrativo para la demo: en un
 * despliegue real vendria de un servicio de inteligencia/planeamiento, no hardcoded.
 */
@Service
public class RiskZoneCatalog {

    private static final List<RiskZone> ZONES = List.of(
            new RiskZone(
                    "z1", "Paso estrecho de Gibraltar",
                    "Corredor maritimo muy transitado y de visibilidad reducida; alta concentracion de trafico en poco espacio.",
                    RiskLevel.HIGH, 36.015, -5.38, 7000),
            new RiskZone(
                    "z2", "Corredor de Punta Carnero",
                    "Tramo costero con cobertura de observacion limitada segun informes historicos de la ruta.",
                    RiskLevel.MEDIUM, 36.05, -5.475, 6000),
            new RiskZone(
                    "z3", "Aproximacion a Ceuta",
                    "Congestion de trafico portuario al llegar; riesgo bajo pero a vigilar.",
                    RiskLevel.LOW, 35.90, -5.31, 4000));

    public List<RiskZone> all() {
        return ZONES;
    }
}
