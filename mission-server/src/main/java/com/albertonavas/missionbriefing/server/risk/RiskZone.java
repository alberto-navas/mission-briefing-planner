package com.albertonavas.missionbriefing.server.risk;

/**
 * Zona geografica de riesgo elevado para el planeamiento de rutas: avisa a una mision
 * propia si su ruta pasa cerca, no calcula ni dirige ningun ataque. Datos ilustrativos
 * (ver catalogo), no inteligencia real.
 */
public record RiskZone(
        String id, String label, String reason, RiskLevel level,
        double latitude, double longitude, double radiusMeters) {
}
