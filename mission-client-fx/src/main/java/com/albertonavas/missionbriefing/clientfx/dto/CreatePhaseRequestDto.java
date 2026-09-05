package com.albertonavas.missionbriefing.clientfx.dto;

public record CreatePhaseRequestDto(String name, int startOffsetMinutes, int endOffsetMinutes, String notes) {
}
