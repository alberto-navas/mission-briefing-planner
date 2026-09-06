package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.MissionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record CreateMissionRequest(
        @NotBlank String name,
        @NotNull MissionType type,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        String description,
        @Valid List<CreateWaypointRequest> waypoints,
        @Valid List<CreatePhaseRequest> phases,
        @Valid List<CreateResourceRequest> resources) {
}
