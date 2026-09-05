package com.albertonavas.missionbriefing.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MissionTest {

    @Test
    void addWaypointLinksBothSides() {
        Mission mission = new Mission("Patrulla costera", MissionType.RECONNAISSANCE,
                Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS), "Reconocimiento de costa");
        Waypoint waypoint = new Waypoint(1, 36.15, -5.35, TaskType.OBSERVE, "Punto de observacion norte");

        mission.addWaypoint(waypoint);

        assertThat(mission.getWaypoints()).containsExactly(waypoint);
        assertThat(waypoint.getMission()).isSameAs(mission);
    }

    @Test
    void addPhaseLinksBothSides() {
        Mission mission = new Mission("Convoy logistico", MissionType.LOGISTICS,
                Instant.now(), Instant.now().plus(4, ChronoUnit.HOURS), "Reabastecimiento");
        MissionPhase phase = new MissionPhase("Transito", 0, 60, "Salida desde base");

        mission.addPhase(phase);

        assertThat(mission.getPhases()).containsExactly(phase);
        assertThat(phase.getMission()).isSameAs(mission);
    }

    @Test
    void addResourceLinksBothSidesAndMarksItUnavailable() {
        Mission mission = new Mission("Escolta urbana", MissionType.ESCORT,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "Escolta de convoy");
        Resource escort = new Resource("Escolta 1", ResourceType.PERSONNEL_TEAM, "BRAVO-1");

        mission.addResource(escort);

        assertThat(mission.getAssignedResources()).containsExactly(escort);
        assertThat(escort.getMission()).isSameAs(mission);
        assertThat(escort.isAvailable()).isFalse();
    }

    @Test
    void assigningResourceToMissionMarksItUnavailable() {
        Mission mission = new Mission("Escolta", MissionType.ESCORT,
                Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), "Escolta de convoy");
        Resource resource = new Resource("Dron 1", ResourceType.DRONE, "ALPHA-1");

        resource.assignTo(mission);

        assertThat(resource.getMission()).isSameAs(mission);
        assertThat(resource.isAvailable()).isFalse();
    }

    @Test
    void unassigningResourceMarksItAvailableAgain() {
        Resource resource = new Resource("Dron 1", ResourceType.DRONE, "ALPHA-1");
        resource.assignTo(new Mission("X", MissionType.ESCORT, Instant.now(), Instant.now(), ""));

        resource.assignTo(null);

        assertThat(resource.getMission()).isNull();
        assertThat(resource.isAvailable()).isTrue();
    }
}
