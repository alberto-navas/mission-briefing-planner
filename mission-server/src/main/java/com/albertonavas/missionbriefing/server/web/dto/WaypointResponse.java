package com.albertonavas.missionbriefing.server.web.dto;

import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;

public record WaypointResponse(
        Long id, int sequenceOrder, double latitude, double longitude, TaskType taskType, String notes) {

    public static WaypointResponse from(Waypoint waypoint) {
        return new WaypointResponse(
                waypoint.getId(), waypoint.getSequenceOrder(), waypoint.getLatitude(),
                waypoint.getLongitude(), waypoint.getTaskType(), waypoint.getNotes());
    }
}
