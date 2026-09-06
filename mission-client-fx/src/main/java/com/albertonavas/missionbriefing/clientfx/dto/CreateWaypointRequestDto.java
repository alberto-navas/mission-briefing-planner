package com.albertonavas.missionbriefing.clientfx.dto;

public record CreateWaypointRequestDto(
        int sequenceOrder, double latitude, double longitude, String taskType, String notes) {
}
