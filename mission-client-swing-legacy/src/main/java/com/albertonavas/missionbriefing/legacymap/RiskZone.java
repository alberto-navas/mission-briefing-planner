package com.albertonavas.missionbriefing.legacymap;

/**
 * Copia local, ligera, de la zona de riesgo que expone mission-server: este modulo no
 * depende del servidor, igual que mission-client-fx tiene sus propios DTOs.
 */
public record RiskZone(
        String id, String label, String reason, RiskLevel level,
        double latitude, double longitude, double radiusMeters) {
}
