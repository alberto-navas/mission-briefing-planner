package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateResourceRequest(@NotBlank String name, @NotNull ResourceType type, String callSign) {
}
