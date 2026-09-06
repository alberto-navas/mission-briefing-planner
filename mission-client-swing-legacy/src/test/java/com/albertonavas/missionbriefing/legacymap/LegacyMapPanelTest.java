package com.albertonavas.missionbriefing.legacymap;

import static org.assertj.core.api.Assertions.assertThat;

import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.jxmapviewer.viewer.GeoPosition;

class LegacyMapPanelTest {

    @Test
    void snapshotMatchesThePanelSize() {
        LegacyMapPanel panel = new LegacyMapPanel();
        panel.setSize(400, 300);

        BufferedImage image = panel.snapshot();

        assertThat(image.getWidth()).isEqualTo(400);
        assertThat(image.getHeight()).isEqualTo(300);
    }

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

    @Test
    void showEscortsRegistersOneMarkerPerEscort() {
        LegacyMapPanel panel = new LegacyMapPanel();
        List<Escort> escorts = List.of(new Escort("e1", "BRAVO-1"), new Escort("e2", "BRAVO-2"));

        panel.showEscorts(escorts, new GeoPosition(36.14, -5.45));

        assertThat(panel.getEscortCount()).isEqualTo(2);
        assertThat(panel.isEscortLost("e1")).isFalse();
    }

    @Test
    void markEscortLostFreezesItAndNotifiesTheListener() {
        LegacyMapPanel panel = new LegacyMapPanel();
        panel.showEscorts(List.of(new Escort("e1", "BRAVO-1")), new GeoPosition(36.14, -5.45));

        AtomicReference<Escort> notified = new AtomicReference<>();
        panel.setEscortLostListener((escort, lastKnownPosition) -> notified.set(escort));

        panel.markEscortLost("e1");

        assertThat(panel.isEscortLost("e1")).isTrue();
        assertThat(notified.get()).isEqualTo(new Escort("e1", "BRAVO-1"));
    }

    @Test
    void markEscortLostTwiceOnlyNotifiesOnce() {
        LegacyMapPanel panel = new LegacyMapPanel();
        panel.showEscorts(List.of(new Escort("e1", "BRAVO-1")), new GeoPosition(36.14, -5.45));

        int[] notifications = {0};
        panel.setEscortLostListener((escort, lastKnownPosition) -> notifications[0]++);

        panel.markEscortLost("e1");
        panel.markEscortLost("e1");

        assertThat(notifications[0]).isEqualTo(1);
    }

    @Test
    void rerouteToExtractionReplacesTheAnimationRouteWithoutStoppingIt() {
        LegacyMapPanel panel = new LegacyMapPanel();
        panel.startConvoyAnimation(List.of(
                new Waypoint(1, 36.15, -5.35, TaskType.TRANSIT, ""),
                new Waypoint(2, 35.95, -5.60, TaskType.TRANSIT, "")));

        panel.rerouteToExtraction(List.of(new GeoPosition(36.0, -5.6), new GeoPosition(36.01, -5.35)));

        assertThat(panel.isAnimationRunning()).isTrue();
    }
}
