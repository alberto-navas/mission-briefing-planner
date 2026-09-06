package com.albertonavas.missionbriefing.server.extraction;

import java.util.List;
import org.springframework.stereotype.Service;

/** Catalogo estatico de puntos de extraccion de ejemplo, ilustrativo (ver RiskZoneCatalog). */
@Service
public class ExtractionPointCatalog {

    private static final List<ExtractionPoint> POINTS = List.of(
            new ExtractionPoint("ep1", "Puerto de Tarifa", 36.0128, -5.6058),
            new ExtractionPoint("ep2", "La Linea de la Concepcion", 36.169, -5.349));

    public List<ExtractionPoint> all() {
        return POINTS;
    }
}
