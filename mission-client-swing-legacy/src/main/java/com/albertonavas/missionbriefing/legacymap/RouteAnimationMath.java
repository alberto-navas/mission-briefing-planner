package com.albertonavas.missionbriefing.legacymap;

import com.albertonavas.missionbriefing.model.Waypoint;
import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Matematica de la animacion del convoy, separada de LegacyMapPanel para poder probarla
 * sin depender de un javax.swing.Timer real ni del hilo de Swing.
 */
final class RouteAnimationMath {

    private RouteAnimationMath() {
    }

    /** Posicion interpolada a lo largo de la ruta ({@code route}, ordenada) para {@code progress} en [0,1]. */
    static GeoPosition interpolate(List<Waypoint> route, double progress) {
        int segments = route.size() - 1;
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        double segmentPosition = clamped * segments;
        int segmentIndex = Math.min((int) segmentPosition, segments - 1);
        double segmentFraction = segmentPosition - segmentIndex;

        Waypoint from = route.get(segmentIndex);
        Waypoint to = route.get(segmentIndex + 1);
        double lat = from.getLatitude() + (to.getLatitude() - from.getLatitude()) * segmentFraction;
        double lon = from.getLongitude() + (to.getLongitude() - from.getLongitude()) * segmentFraction;
        return new GeoPosition(lat, lon);
    }

    /** Primera zona (en el orden dado) cuyo radio cubre {@code position}, o null si ninguna. */
    static RiskZone findContainingZone(GeoPosition position, List<RiskZone> zones) {
        for (RiskZone zone : zones) {
            double distance = GeoMath.distanceMeters(
                    position.getLatitude(), position.getLongitude(), zone.latitude(), zone.longitude());
            if (distance <= zone.radiusMeters()) {
                return zone;
            }
        }
        return null;
    }
}
