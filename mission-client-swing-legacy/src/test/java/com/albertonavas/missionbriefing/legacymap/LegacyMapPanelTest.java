package com.albertonavas.missionbriefing.legacymap;

import static org.assertj.core.api.Assertions.assertThat;

import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyMapPanelTest {

    @Test
    void showWaypointsAddsOneMarkerPerWaypoint() {
        LegacyMapPanel panel = new LegacyMapPanel();
        List<Waypoint> waypoints = List.of(
                new Waypoint(1, 36.15, -5.35, TaskType.OBSERVE, "Norte"),
                new Waypoint(2, 35.95, -5.60, TaskType.TRANSIT, "Sur"));

        panel.showWaypoints(waypoints);

        assertThat(panel.getWaypointMarkerCount()).isEqualTo(2);
    }

    @Test
    void startsWithNoMarkers() {
        LegacyMapPanel panel = new LegacyMapPanel();

        assertThat(panel.getWaypointMarkerCount()).isZero();
    }
}
