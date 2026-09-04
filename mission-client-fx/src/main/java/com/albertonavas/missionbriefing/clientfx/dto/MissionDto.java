package com.albertonavas.missionbriefing.clientfx.dto;

import java.time.Instant;
import java.util.List;

public record MissionDto(
        Long id,
        String name,
        String type,
        String status,
        Instant startTime,
        Instant endTime,
        String description,
        List<WaypointDto> waypoints,
        List<PhaseDto> phases) {

    @Override
    public String toString() {
        return "%s (%s)".formatted(name, type);
    }
}
