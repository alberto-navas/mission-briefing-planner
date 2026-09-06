package com.albertonavas.missionbriefing.clientfx.dto;

/** Espejo del ResourceResponse del servidor. */
public record ResourceDto(Long id, String name, String type, String callSign, boolean available) {
}
