package com.albertonavas.missionbriefing.server.extraction;

/**
 * Punto de extraccion seguro al que redirigir una mision propia si su seguridad se ve
 * comprometida (p.ej. se pierde un escolta). No es un objetivo ni una zona de combate:
 * es a donde se retira la propia mision, ver "Limites eticos" en el README.
 */
public record ExtractionPoint(String id, String label, double latitude, double longitude) {
}
