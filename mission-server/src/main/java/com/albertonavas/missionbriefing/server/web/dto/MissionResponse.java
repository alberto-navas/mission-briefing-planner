package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.Mission;
import com.albertonavas.missionbriefing.model.MissionStatus;
import com.albertonavas.missionbriefing.model.MissionType;
import java.time.Instant;
import java.util.List;

public record MissionResponse(
        Long id,
        String name,
        MissionType type,
        MissionStatus status,
        Instant startTime,
        Instant endTime,
        String description,
        List<WaypointResponse> waypoints,
        List<PhaseResponse> phases) {

    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getName(),
                mission.getType(),
                mission.getStatus(),
                mission.getStartTime(),
                mission.getEndTime(),
                mission.getDescription(),
                mission.getWaypoints().stream().map(WaypointResponse::from).toList(),
                mission.getPhases().stream().map(PhaseResponse::from).toList());
    }
}
