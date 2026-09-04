package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.MissionPhase;

public record PhaseResponse(Long id, String name, int startOffsetMinutes, int endOffsetMinutes, String notes) {

    public static PhaseResponse from(MissionPhase phase) {
        return new PhaseResponse(
                phase.getId(), phase.getName(), phase.getStartOffsetMinutes(),
                phase.getEndOffsetMinutes(), phase.getNotes());
    }
}
