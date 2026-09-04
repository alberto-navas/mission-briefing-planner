package com.albertonavas.missionbriefing.server.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePhaseRequest(
        @NotBlank String name,
        int startOffsetMinutes,
        int endOffsetMinutes,
        String notes) {
}
