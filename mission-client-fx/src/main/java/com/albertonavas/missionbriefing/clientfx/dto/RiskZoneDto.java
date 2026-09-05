package com.albertonavas.missionbriefing.clientfx.dto;

/** Espejo del RiskZone que expone mission-server. */
public record RiskZoneDto(
        String id, String label, String reason, String level,
        double latitude, double longitude, double radiusMeters) {
}
