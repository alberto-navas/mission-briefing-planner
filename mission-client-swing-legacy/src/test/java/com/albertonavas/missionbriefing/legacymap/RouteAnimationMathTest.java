package com.albertonavas.missionbriefing.legacymap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;
import org.junit.jupiter.api.Test;

class RouteAnimationMathTest {

    // Tres puntos equiespaciados en longitud (misma latitud): dos tramos de igual distancia.
    private final List<GeoPosition> evenRoute = List.of(
            new GeoPosition(36.00, -5.00),
            new GeoPosition(36.00, -4.00),
            new GeoPosition(36.00, -3.00));

    @Test
    void progressZeroIsFirstPoint() {
        GeoPosition p = RouteAnimationMath.interpolate(evenRoute, 0.0);

        assertThat(p.getLongitude()).isCloseTo(-5.00, within(1e-6));
    }

    @Test
    void progressOneIsLastPoint() {
        GeoPosition p = RouteAnimationMath.interpolate(evenRoute, 1.0);

        assertThat(p.getLongitude()).isCloseTo(-3.00, within(1e-6));
    }

    @Test
    void progressHalfwayIsMiddlePointWhenSegmentsAreEqualLength() {
        GeoPosition p = RouteAnimationMath.interpolate(evenRoute, 0.5);

        assertThat(p.getLongitude()).isCloseTo(-4.00, within(1e-6));
    }

    @Test
    void progressIsWeightedByDistanceNotByPointCount() {
        // Muchos puntos apretados en el primer tramo, uno solo en el segundo tramo (el
        // doble de largo): a progress=0.5 el convoy deberia estar a mitad de distancia
        // total, es decir dentro del primer tramo (denso en puntos, corto en distancia),
        // no en el punto intermedio por indice.
        List<GeoPosition> unevenRoute = List.of(
                new GeoPosition(36.00, -5.00),
                new GeoPosition(36.00, -4.99),
                new GeoPosition(36.00, -4.98),
                new GeoPosition(36.00, -4.60)); // tramo final mucho mas largo

        GeoPosition p = RouteAnimationMath.interpolate(unevenRoute, 0.5);

        assertThat(p.getLongitude()).isLessThan(-4.80); // aun en el tramo corto y denso
    }

    @Test
    void singlePointRouteReturnsThatPoint() {
        GeoPosition p = RouteAnimationMath.interpolate(List.of(new GeoPosition(1.0, 2.0)), 0.7);

        assertThat(p.getLatitude()).isEqualTo(1.0);
        assertThat(p.getLongitude()).isEqualTo(2.0);
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
