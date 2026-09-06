package com.albertonavas.missionbriefing.clientfx.dto;

/** Espejo del ExtractionPoint que expone mission-server. */
public record ExtractionPointDto(String id, String label, double latitude, double longitude) {
}
