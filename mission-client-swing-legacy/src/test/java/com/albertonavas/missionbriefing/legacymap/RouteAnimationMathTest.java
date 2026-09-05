package com.albertonavas.missionbriefing.legacymap;

import static org.assertj.core.api.Assertions.assertThat;

import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

class RouteAnimationMathTest {

    private final List<Waypoint> route = List.of(
            new Waypoint(1, 36.00, -5.00, TaskType.TRANSIT, "inicio"),
            new Waypoint(2, 36.00, -4.00, TaskType.TRANSIT, "medio"),
            new Waypoint(3, 36.00, -3.00, TaskType.TRANSIT, "fin"));

    @Test
    void progressZeroIsFirstWaypoint() {
        GeoPosition p = RouteAnimationMath.interpolate(route, 0.0);

        assertThat(p.getLatitude()).isEqualTo(36.00);
        assertThat(p.getLongitude()).isEqualTo(-5.00);
    }

    @Test
    void progressOneIsLastWaypoint() {
        GeoPosition p = RouteAnimationMath.interpolate(route, 1.0);

        assertThat(p.getLongitude()).isEqualTo(-3.00);
    }

    @Test
    void progressHalfwayIsMiddleWaypoint() {
        // 3 waypoints = 2 segmentos; progress 0.5 cae justo en el limite del primer segmento.
        GeoPosition p = RouteAnimationMath.interpolate(route, 0.5);

        assertThat(p.getLongitude()).isEqualTo(-4.00);
    }

    @Test
    void progressQuarterIsMidwayThroughFirstSegment() {
        GeoPosition p = RouteAnimationMath.interpolate(route, 0.25);

        assertThat(p.getLongitude()).isEqualTo(-4.50);
    }

    @Test
    void findContainingZoneReturnsZoneWithinRadius() {
        RiskZone zone = new RiskZone("z1", "Zona", "motivo", RiskLevel.HIGH, 36.00, -4.50, 20_000);

        RiskZone found = RouteAnimationMath.findContainingZone(new GeoPosition(36.00, -4.50), List.of(zone));

        assertThat(found).isEqualTo(zone);
    }

    @Test
    void findContainingZoneReturnsNullWhenOutsideEveryRadius() {
        RiskZone zone = new RiskZone("z1", "Zona", "motivo", RiskLevel.HIGH, 36.00, -4.50, 100);

        RiskZone found = RouteAnimationMath.findContainingZone(new GeoPosition(37.00, -1.00), List.of(zone));

        assertThat(found).isNull();
    }
}
