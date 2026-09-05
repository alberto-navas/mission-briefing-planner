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

    @Test
    void startConvoyAnimationRunsUntilResetOrStopped() {
        LegacyMapPanel panel = new LegacyMapPanel();
        List<Waypoint> waypoints = List.of(
                new Waypoint(1, 36.15, -5.35, TaskType.TRANSIT, "Norte"),
                new Waypoint(2, 35.95, -5.60, TaskType.TRANSIT, "Sur"));

        panel.startConvoyAnimation(waypoints);
        assertThat(panel.isAnimationRunning()).isTrue();

        panel.pauseConvoyAnimation();
        assertThat(panel.isAnimationRunning()).isFalse();

        panel.resetConvoyAnimation();
        assertThat(panel.isAnimationRunning()).isFalse();
    }

    @Test
    void startConvoyAnimationWithFewerThanTwoWaypointsDoesNothing() {
        LegacyMapPanel panel = new LegacyMapPanel();

        panel.startConvoyAnimation(List.of(new Waypoint(1, 36.0, -5.0, TaskType.HOLD, "unico")));

        assertThat(panel.isAnimationRunning()).isFalse();
    }

    @Test
    void showRiskZonesDoesNotThrow() {
        LegacyMapPanel panel = new LegacyMapPanel();
        RiskZone zone = new RiskZone("z1", "Zona", "motivo", RiskLevel.MEDIUM, 36.0, -5.5, 5000);

        panel.showRiskZones(List.of(zone));

        assertThat(panel.getWaypointMarkerCount()).isZero();
    }
}
