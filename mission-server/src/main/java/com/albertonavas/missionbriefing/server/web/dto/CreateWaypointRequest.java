package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.TaskType;
import jakarta.validation.constraints.NotNull;

public record CreateWaypointRequest(
        int sequenceOrder,
        double latitude,
        double longitude,
        @NotNull TaskType taskType,
        String notes) {
}
