package com.albertonavas.missionbriefing.clientfx.dto;

import java.time.Instant;
import java.util.List;

public record CreateMissionRequestDto(
        String name,
        String type,
        Instant startTime,
        Instant endTime,
        String description,
        List<CreateWaypointRequestDto> waypoints,
        List<CreatePhaseRequestDto> phases,
        List<CreateResourceRequestDto> resources) {
}
