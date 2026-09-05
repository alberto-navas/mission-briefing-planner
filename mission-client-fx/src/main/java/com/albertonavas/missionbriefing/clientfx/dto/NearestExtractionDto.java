package com.albertonavas.missionbriefing.clientfx.dto;

import java.util.List;

/** Espejo del NearestExtractionResult que expone mission-server. */
public record NearestExtractionDto(ExtractionPointDto point, List<GeoPointDto> route) {
}
