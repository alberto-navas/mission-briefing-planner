package com.albertonavas.missionbriefing.clientfx.dto;

/** Espejo del WaypointResponse del servidor: el cliente no depende del modulo JPA. */
public record WaypointDto(
        Long id, int sequenceOrder, double latitude, double longitude, String taskType, String notes) {
}
