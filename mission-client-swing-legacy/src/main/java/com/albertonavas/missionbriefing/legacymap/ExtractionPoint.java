package com.albertonavas.missionbriefing.legacymap;

/** Copia local, ligera, del punto de extraccion que expone mission-server. */
public record ExtractionPoint(String id, String label, double latitude, double longitude) {
}
